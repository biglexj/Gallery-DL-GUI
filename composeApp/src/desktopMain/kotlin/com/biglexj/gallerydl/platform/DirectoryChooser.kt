package com.biglexj.gallerydl.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.swing.JFileChooser
import javax.swing.UIManager

object DirectoryChooser {
    suspend fun chooseDirectory(currentPath: String?): String? = withContext(Dispatchers.IO) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (e: Exception) {
            // Fallback to default Swing look and feel
        }
        val chooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            dialogTitle = "Seleccionar Carpeta de Guardado"
            currentPath?.let {
                val f = java.io.File(it)
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
}
