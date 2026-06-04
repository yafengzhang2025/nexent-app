package com.nexent.app.ui.chat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.nexent.app.R
import com.nexent.app.databinding.ActivityChatBinding
import java.io.File
import java.util.Locale

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var messageAdapter: MessageAdapter

    private var pendingCameraUri: Uri? = null

    // File picker
    private val pickFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.sendImageMessage(it, "请分析这个文件")
        }
    }

    // Camera
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            pendingCameraUri?.let { uri ->
                viewModel.sendImageMessage(uri, "请分析这张图片")
            }
        }
    }

    // Camera permission
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
        else Toast.makeText(this, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
    }

    // Voice input
    private val voiceLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        val spokenText = matches?.firstOrNull() ?: return@registerForActivityResult
        binding.etMessage.setText(spokenText)
        if (spokenText.isNotBlank()) {
            viewModel.sendMessage(spokenText)
            binding.etMessage.setText("")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val agentName = intent.getStringExtra(EXTRA_AGENT_NAME) ?: "AI Assistant"
        val agentTitle = intent.getStringExtra(EXTRA_AGENT_TITLE) ?: agentName

        setupHeader(agentTitle)
        setupRecyclerView()
        setupInputArea()
        observeViewModel()

        viewModel.init(agentName)
    }

    private fun setupHeader(agentTitle: String) {
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Show Chinese title
        binding.tvAgentSubtitle.text = agentTitle

        // Assign avatar color based on agent name hash
        val avatarIndex = (agentTitle.hashCode().mod(avatarBgList.size).let {
            if (it < 0) it + avatarBgList.size else it
        })
        binding.flChatAvatar.setBackgroundResource(avatarBgList[avatarIndex])

        // Set dynamic input hint with Chinese name
        binding.etMessage.hint = getString(R.string.input_hint, agentTitle)
    }

    companion object {
        const val EXTRA_AGENT_NAME = "extra_agent_name"
        const val EXTRA_AGENT_TITLE = "extra_agent_title"
        const val EXTRA_AGENT_DESC = "extra_agent_desc"

        private val avatarBgList = intArrayOf(
            R.drawable.bg_avatar_01, R.drawable.bg_avatar_02,
            R.drawable.bg_avatar_03, R.drawable.bg_avatar_04,
            R.drawable.bg_avatar_05, R.drawable.bg_avatar_06,
            R.drawable.bg_avatar_07, R.drawable.bg_avatar_08
        )
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter()
        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }
    }

    private fun setupInputArea() {
        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text?.toString()?.trim() ?: return@setOnClickListener
            if (text.isNotBlank()) {
                viewModel.sendMessage(text)
                binding.etMessage.setText("")
            }
        }

        // Voice input via the add button
        binding.btnAdd.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "说出你想问的问题...")
            }
            voiceLauncher.launch(intent)
        }

        // File picker via image button
        binding.btnImage.setOnClickListener {
            pickFileLauncher.launch("image/*")
        }

        // Camera via edit button
        binding.btnEdit.setOnClickListener {
            checkCameraPermission()
        }

        // Emoji button - insert emoji at cursor (simple emoji picker)
        binding.btnEmoji.setOnClickListener {
            val current = binding.etMessage.text ?: return@setOnClickListener
            current.insert(current.length, "😊")
        }

        // Mode dropdown selector
        binding.modeSelector.setOnClickListener { showModePopup() }
    }

    private fun showModePopup() {
        val popup = android.widget.PopupMenu(this, binding.modeSelector)
        popup.menu.add(0, 0, 0, getString(R.string.mode_quick))
        popup.menu.add(0, 1, 1, getString(R.string.mode_deep))
        popup.setOnMenuItemClickListener { item ->
            val mode = if (item.itemId == 1) "deep" else "quick"
            selectMode(mode)
            true
        }
        popup.show()
    }

    private fun selectMode(mode: String) {
        if (mode == "deep") {
            binding.tvModeLabel.text = getString(R.string.mode_deep)
        } else {
            binding.tvModeLabel.text = getString(R.string.mode_quick)
        }
        viewModel.setThinkMode(mode)
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        val photoFile = File(externalCacheDir, "nexent_photo_${System.currentTimeMillis()}.jpg")
        pendingCameraUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )
        takePictureLauncher.launch(pendingCameraUri!!)
    }

    private fun observeViewModel() {
        viewModel.messages.observe(this) { messages ->
            messageAdapter.submitList(messages.toList()) {
                if (messages.isNotEmpty()) {
                    binding.rvMessages.smoothScrollToPosition(messages.size - 1)
                }
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.btnSend.isEnabled = !isLoading
        }

        viewModel.error.observe(this) { error ->
            if (!error.isNullOrBlank()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            }
        }
    }
}
