package org.jetbrains.kotlin.demo

import com.sun.javafx.tools.packager.Log
import lombok.extern.slf4j.Slf4j
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.File

@RestController
@Slf4j
class TranscribeController {


    @PostMapping("/transcribe")
    fun transcribe(@RequestParam("file") file: MultipartFile): File {

        val outDir = "./data/examples/output/"
        val outputFile = File(outDir + file.name.split(".").first() + ".mxml")
        val args = arrayOf("-batch", "-export", "-output", outDir, "--", file.originalFilename)
        Log.debug(file.originalFilename + "will be transcribed!" )
//        org.audiveris.omr.Main.main(args)
        return outputFile
    }


}