package org.jetbrains.kotlin.demo


import org.apache.commons.logging.LogFactory
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.junit.Before
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.web.context.WebApplicationContext
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders


@RunWith(SpringRunner::class)
@AutoConfigureMockMvc
@SpringBootTest
@WebAppConfiguration
class TranscribeControllerTest {
    protected var logger = LogFactory.getLog(TranscribeControllerTest::class.java)

    protected var mockMvc: MockMvc? = null

    @Autowired
    protected var wac: WebApplicationContext? = null

    @Before
    fun setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build()
    }



    @Test
    fun shouldTranscribeMusicPics() {
        var firstFile = MockMultipartFile("file", "baq", "text/plain", "some xml".toByteArray())
//        val responseString = mockMvc!!
//                .perform(MockMvcRequestBuilders.multipart("/upload"))
    }

}