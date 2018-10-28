package com.audiveris.wrapper

import com.sun.javafx.tools.packager.Log
import lombok.extern.slf4j.Slf4j
import org.audiveris.omr.log.LogUtil
import org.audiveris.omr.util.FileUtil
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.File
import org.slf4j.LoggerFactory

@RestController
@Slf4j
class TranscribeController {

    @PostMapping("/transcribe")
    fun transcribe(@RequestParam("file") file: MultipartFile): String {
        if (! file.isEmpty) {
            val outDir = "data/examples/output/"
            val tempFile = File.createTempFile("temp", ".png")
            val tempFileName = tempFile.name.split(".").first()
            val outputFile = File("$outDir/$tempFileName/$tempFileName.xml")

            file.transferTo(tempFile)
            Log.info("temp file created : " +  tempFile.absolutePath)
            val args = arrayOf("-batch", "-export", "-output", outDir, "--", tempFile.absolutePath )
            Log.debug(file.originalFilename + "will be transcribed!" )
            return try {
                org.audiveris.omr.Main.main(args)
                outputFile.absolutePath
            } catch (e: Exception) {
                logger.debug("transcribe file ${file.originalFilename} failed ")
                e.printStackTrace()
                ""
            }
        }
        return ""
    }

    companion object LogHelper{
        val logger  = LoggerFactory.getLogger("TranscribeController")
    }
}