package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.VaultDatabase
import com.example.data.VaultItem
import com.example.data.VaultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest

enum class UnlockState {
    SETUP_PIN,    // Initial setup select PIN
    CONFIRM_PIN,  // Initial setup confirm PIN
    LOCKED,       // Waiting for PIN to be entered on calculator
    UNLOCKED      // Vault unlocked
}

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("vault_prefs", Context.MODE_PRIVATE)
    private val repository: VaultRepository

    // UI States
    private val _unlockState = MutableStateFlow(UnlockState.LOCKED)
    val unlockState: StateFlow<UnlockState> = _unlockState.asStateFlow()

    private val _calculatorDisplay = MutableStateFlow("")
    val calculatorDisplay: StateFlow<String> = _calculatorDisplay.asStateFlow()

    private val _calculatorResult = MutableStateFlow("")
    val calculatorResult: StateFlow<String> = _calculatorResult.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _photos = MutableStateFlow<List<VaultItem>>(emptyList())
    val photos: StateFlow<List<VaultItem>> = _photos.asStateFlow()

    private val _videos = MutableStateFlow<List<VaultItem>>(emptyList())
    val videos: StateFlow<List<VaultItem>> = _videos.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    // Preview state
    private val _selectedItem = MutableStateFlow<VaultItem?>(null)
    val selectedItem: StateFlow<VaultItem?> = _selectedItem.asStateFlow()

    // Temporary storage for PIN setup
    private var tempPin = ""

    init {
        val database = VaultDatabase.getDatabase(application)
        repository = VaultRepository(database.vaultDao())

        // Check if password has been configured already
        val savedHash = sharedPrefs.getString("pin_hash", null)
        if (savedHash == null) {
            _unlockState.value = UnlockState.SETUP_PIN
        } else {
            _unlockState.value = UnlockState.LOCKED
        }

        // Fetch user files in background
        observeMediaItems()
    }

    private fun observeMediaItems() {
        viewModelScope.launch {
            repository.getItemsByType("PHOTO")
                .catch { Log.e("VaultViewModel", "Error loading photos", it) }
                .collect { _photos.value = it }
        }
        viewModelScope.launch {
            repository.getItemsByType("VIDEO")
                .catch { Log.e("VaultViewModel", "Error loading videos", it) }
                .collect { _videos.value = it }
        }
    }

    // Hash Helper (SHA-256)
    private fun hashString(input: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(input.toByteArray())
            digest.fold("") { str, it -> str + "%02x".format(it) }
        } catch (e: Exception) {
            input // Fallback
        }
    }

    fun showMessage(message: String) {
        _feedbackMessage.value = message
    }

    fun clearMessage() {
        _feedbackMessage.value = null
    }

    /**
     * Handles Calculator button feedback
     */
    fun onCalculatorButtonPress(char: String) {
        val currentDisplay = _calculatorDisplay.value

        when (char) {
            "C" -> {
                _calculatorDisplay.value = ""
                _calculatorResult.value = ""
            }
            "DEL" -> {
                if (currentDisplay.isNotEmpty()) {
                    _calculatorDisplay.value = currentDisplay.dropLast(1)
                }
            }
            "=" -> {
                handleEqualsAction()
            }
            else -> {
                // Prevent duplicate consecutive operation characters for simple safety
                if (isOperator(char) && currentDisplay.isNotEmpty() && isOperator(currentDisplay.last().toString())) {
                    _calculatorDisplay.value = currentDisplay.dropLast(1) + char
                } else {
                    _calculatorDisplay.value = currentDisplay + char
                }
            }
        }
    }

    private fun isOperator(s: String): Boolean {
        return s == "+" || s == "-" || s == "×" || s == "÷" || s == "*" || s == "/"
    }

    /**
     * Logic for "=" button: Evaluates math OR processes PIN actions based on unlockState
     */
    private fun handleEqualsAction() {
        val expression = _calculatorDisplay.value.trim()
        if (expression.isEmpty()) return

        val state = _unlockState.value
        if (state == UnlockState.SETUP_PIN || state == UnlockState.CONFIRM_PIN || state == UnlockState.LOCKED) {
            // Check if the input is a pure digit PIN code (4 to 8 digits)
            val isPurePin = expression.all { it.isDigit() } && expression.length in 4..8

            if (isPurePin) {
                processPinAction(expression)
                return
            }
        }

        // Otherwise, perform standard math calculation for normal calculator activity
        val result = evaluateMathExpression(expression)
        _calculatorResult.value = result
        // Update display to result so user can continue calculations
        _calculatorDisplay.value = result
    }

    /**
     * Process PIN setup or validation
     */
    private fun processPinAction(pin: String) {
        when (_unlockState.value) {
            UnlockState.SETUP_PIN -> {
                tempPin = pin
                _unlockState.value = UnlockState.CONFIRM_PIN
                _calculatorDisplay.value = ""
                _calculatorResult.value = ""
                showMessage("הזן שוב את הקוד לאישור")
            }
            UnlockState.CONFIRM_PIN -> {
                if (pin == tempPin) {
                    val hashed = hashString(pin)
                    sharedPrefs.edit().putString("pin_hash", hashed).apply()
                    _unlockState.value = UnlockState.UNLOCKED
                    _calculatorDisplay.value = ""
                    _calculatorResult.value = ""
                    showMessage("הקוד הוגדר בהצלחה! הכספת פתוחה")
                } else {
                    _unlockState.value = UnlockState.SETUP_PIN
                    _calculatorDisplay.value = ""
                    _calculatorResult.value = ""
                    showMessage("הקודים אינם תואמים. התחל מחדש")
                }
            }
            UnlockState.LOCKED -> {
                val savedHash = sharedPrefs.getString("pin_hash", null)
                val inputHash = hashString(pin)
                if (savedHash == inputHash) {
                    _unlockState.value = UnlockState.UNLOCKED
                    _calculatorDisplay.value = ""
                    _calculatorResult.value = ""
                    showMessage("הכספת נפתחה")
                } else {
                    // It was a pure number but not the password, let's just calculate it as math so decoy works seamlessly!
                    val result = evaluateMathExpression(pin)
                    _calculatorResult.value = result
                    _calculatorDisplay.value = result
                }
            }
            UnlockState.UNLOCKED -> {
                // Already unlocked
            }
        }
    }

    /**
     * Pure Kotlin simple math expression parser for decoy calculator.
     * Supports +, -, *, /
     */
    private fun evaluateMathExpression(expr: String): String {
        try {
            // Replace visual multiply/divide tokens
            var cleaned = expr.replace("×", "*").replace("÷", "/")
            
            // Evaluates simple expressions with left-to-right calculation for stability
            // Parse tokens
            val tokens = mutableListOf<String>()
            var numberAccumulator = ""
            for (char in cleaned) {
                if (char.isDigit() || char == '.') {
                    numberAccumulator += char
                } else if (char == '+' || char == '-' || char == '*' || char == '/') {
                    if (numberAccumulator.isNotEmpty()) {
                        tokens.add(numberAccumulator)
                        numberAccumulator = ""
                    }
                    tokens.add(char.toString())
                }
            }
            if (numberAccumulator.isNotEmpty()) {
                tokens.add(numberAccumulator)
            }

            if (tokens.isEmpty()) return "0"

            // 1. Process multiplication and division first (standard operator precedence)
            var i = 0
            while (i < tokens.size) {
                if (tokens[i] == "*" || tokens[i] == "/") {
                    if (i > 0 && i < tokens.size - 1) {
                        val num1 = tokens[i - 1].toDoubleOrNull() ?: 0.0
                        val num2 = tokens[i + 1].toDoubleOrNull() ?: 0.0
                        val op = tokens[i]
                        val res = if (op == "*") num1 * num2 else {
                            if (num2 == 0.0) return "שגיאה" // Division by zero
                            num1 / num2
                        }
                        tokens.removeAt(i + 1)
                        tokens.removeAt(i)
                        tokens[i - 1] = res.toString()
                        i-- // Adjust index
                    } else {
                        return "שגיאה"
                    }
                }
                i++
            }

            // 2. Process addition and subtraction
            var total = tokens.firstOrNull()?.toDoubleOrNull() ?: 0.0
            var idx = 1
            while (idx < tokens.size) {
                val op = tokens[idx]
                if (idx + 1 < tokens.size) {
                    val nextNum = tokens[idx + 1].toDoubleOrNull() ?: 0.0
                    total = if (op == "+") {
                        total + nextNum
                    } else if (op == "-") {
                        total - nextNum
                    } else {
                        total
                    }
                    idx += 2
                } else {
                    break
                }
            }

            // Return clean number if decimal has .0
            return if (total % 1 == 0.0) {
                total.toLong().toString()
            } else {
                String.format("%.4f", total).trimEnd('0').trimEnd('.')
            }
        } catch (e: Exception) {
            return "שגיאה"
        }
    }

    /**
     * Lock the app back to decoy calculator
     */
    fun lockVault() {
        _unlockState.value = UnlockState.LOCKED
        _calculatorDisplay.value = ""
        _calculatorResult.value = ""
        _selectedItem.value = null
    }

    /**
     * Select a media item to display fullscreen/player
     */
    fun selectItem(item: VaultItem?) {
        _selectedItem.value = item
    }

    /**
     * Import multiple selected images or videos
     */
    fun importMultipleMedia(context: Context, uris: List<Uri>, isVideo: Boolean) {
        if (uris.isEmpty()) return
        _isImporting.value = true
        viewModelScope.launch {
            var successCount = 0
            for (uri in uris) {
                val success = repository.importMedia(context, uri, isVideo)
                if (success) successCount++
            }
            _isImporting.value = false
            if (successCount == uris.size) {
                showMessage("כל הקבצים ($successCount) יובאו לכספת בהצלחה!")
            } else if (successCount > 0) {
                showMessage("יובאו בהצלחה $successCount מתוך ${uris.size} קבצים.")
            } else {
                showMessage("ייבוא הקבצים נכשל. בדוק הרשאות.")
            }
        }
    }

    /**
     * Delete a media item permanently
     */
    fun deleteItem(context: Context, item: VaultItem) {
        viewModelScope.launch {
            val success = repository.deleteMedia(context, item)
            if (success) {
                if (_selectedItem.value?.id == item.id) {
                    _selectedItem.value = null
                }
                showMessage("הקובץ נמחק לצמיתות מהכספת")
            } else {
                showMessage("מחיקת הקובץ נכשלה")
            }
        }
    }

    /**
     * Retrieve the actual File object of standard media in internal storage
     */
    fun getFileForItem(context: Context, item: VaultItem): File {
        return repository.getFileForItem(context, item)
    }

    /**
     * Resets the entire passcode (for development/testing or user reset)
     */
    fun resetPasscode() {
        sharedPrefs.edit().remove("pin_hash").apply()
        _unlockState.value = UnlockState.SETUP_PIN
        _calculatorDisplay.value = ""
        _calculatorResult.value = ""
        showMessage("הקוד אופס. נא להגדיר קוד חדש")
    }
}
