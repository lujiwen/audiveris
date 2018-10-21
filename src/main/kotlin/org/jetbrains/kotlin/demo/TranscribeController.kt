package org.jetbrains.kotlin.demo

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.atomic.AtomicLong

@RestController
class TranscribeController {

    val counter = AtomicLong()

    @GetMapping("/transcribe")
    fun transcribe() {
        val args = arrayOf("-batch", "-export", "-output", "./data/examples/output", "--", "./data/examples/carmen.png")
        org.audiveris.omr.Main.main(args)
    }


}