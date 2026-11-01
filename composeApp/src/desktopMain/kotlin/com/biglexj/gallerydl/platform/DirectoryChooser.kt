package com.biglexj.gallerydl.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.JFileChooser
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter

object DirectoryChooser {
    suspend fun chooseDirectory(currentPath: String?): String? = withContext(Dispatchers.IO) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (_: Exception) {
            // Fallback to default Swing look and feel
        }
        val chooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            dialogTitle = "Seleccionar Carpeta de Guardado"
            currentPath?.let {
                val f = File(it)
                if (f.exists()) {
                    currentDirectory = f
                }
            }
        }
        val result = chooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile.absolutePath
        } else {
            null
        }
    }

    suspend fun chooseCookieFile(): String? = withContext(Dispatchers.IO) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (_: Exception) {
            // Fallback to default Swing look and feel
        }
        val chooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.FILES_ONLY
            dialogTitle = "Seleccionar Archivo de Cookies (*.txt)"
            fileFilter = FileNameExtensionFilter("Archivos de texto (*.txt)", "txt")
        }
        val result = chooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile.absolutePath
        } else {
            null
        }
    }
}
