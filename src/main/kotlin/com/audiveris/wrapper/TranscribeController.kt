package com.audiveris.wrapper

import lombok.extern.slf4j.Slf4j
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
    fun transcribe(@RequestParam("file") file: MultipartFile,
                   @RequestParam("userId") userId: String): String {
        if (! file.isEmpty) {
            val outDir = "data/sheet-xml/$userId"
            val tempFile = File.createTempFile("temp", ".png")
            val tempFileName = tempFile.name.split(".").first()
            val outputFile = File("$outDir/$tempFileName/$tempFileName.xml")

            file.transferTo(tempFile)
            val args = arrayOf("-batch", "-export", "-output", outDir, "--", tempFile.absolutePath )
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