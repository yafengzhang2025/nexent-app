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

        setupToolbar(agentName)
        setupRecyclerView()
        setupInputArea()
        observeViewModel()

        viewModel.init(agentName)
    }

    private fun setupToolbar(agentName: String) {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = agentName
            setDisplayHomeAsUpEnabled(true)
        }
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
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

        binding.btnVoice.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "说出你想问的问题...")
            }
            voiceLauncher.launch(intent)
        }

        binding.btnAttach.setOnClickListener {
            pickFileLauncher.launch("image/*")
        }

        binding.btnCamera.setOnClickListener {
            checkCameraPermission()
        }
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

    companion object {
        const val EXTRA_AGENT_NAME = "extra_agent_name"
        const val EXTRA_AGENT_DESC = "extra_agent_desc"
    }
}
