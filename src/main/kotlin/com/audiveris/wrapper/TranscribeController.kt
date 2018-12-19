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
                   @RequestParam ("userId") userId: String): String {
        if (! file.isEmpty) {
            val outDir = File("data/sheet-xml/$userId")
            if(! outDir.exists()) {
                outDir.mkdir()
            }

            val splits = file.originalFilename.split(".")
            val postfix = splits[splits.size-1]
            val sheetName = splits[0]
            val tempFile = File(System.getProperty("java.io.tmpdir") + "/$userId/$sheetName.$postfix")
            tempFile.parentFile.mkdirs()
            tempFile.createNewFile()

            val outputFile = File("$outDir/$sheetName/$sheetName.xml")

            file.transferTo(tempFile)
            val args = arrayOf("-batch", "-export", "-output", outDir.absolutePath, "--", tempFile.absolutePath )
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