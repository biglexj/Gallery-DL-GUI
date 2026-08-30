package com.biglexj.gallerydl.platform.synapse

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SynapseUriParserTest {

    @Test
    fun `parses focus action keyword`() {
        val action = SynapseUriParser.parse("FOCUS")
        assertNotNull(action)
        assertIs<SynapseAction.Focus>(action)
    }

    @Test
    fun `parses direct http and https urls`() {
        val actionHttp = SynapseUriParser.parse("http://example.com/art/123")
        assertNotNull(actionHttp)
        assertIs<SynapseAction.Download>(actionHttp)
        assertEquals("http://example.com/art/123", actionHttp.url)
        assertEquals("DirectUrl", actionHttp.source)

        val actionHttps = SynapseUriParser.parse("https://danbooru.donmai.us/posts/12345")
        assertNotNull(actionHttps)
        assertIs<SynapseAction.Download>(actionHttps)
        assertEquals("https://danbooru.donmai.us/posts/12345", actionHttps.url)
    }

    @Test
    fun `parses gallerydl protocol uri schemes`() {
        val uri = "gallerydl://download?url=https%3A%2F%2Fwww.deviantart.com%2Fartist%2Fart%2F123&dest=D%3A%5CDownloads&silent=true"
        val action = SynapseUriParser.parse(uri)
        assertNotNull(action)
        assertIs<SynapseAction.Download>(action)
        assertEquals("https://www.deviantart.com/artist/art/123", action.url)
        assertEquals("D:\\Downloads", action.destination)
        assertEquals(true, action.silent)

        val enqueueUri = "gallerydl://enqueue?url=https%3A%2F%2Fpixiv.net%2Fartworks%2F987654"
        val enqueueAction = SynapseUriParser.parse(enqueueUri)
        assertNotNull(enqueueAction)
        assertIs<SynapseAction.Enqueue>(enqueueAction)
        assertEquals("https://pixiv.net/artworks/987654", enqueueAction.url)
    }

    @Test
    fun `parses aurora-synapse protocol uri schemes`() {
        val uri = "aurora-synapse://gallerydl/download?url=https%3A%2F%2Fartstation.com%2Fartwork%2Fabc"
        val action = SynapseUriParser.parse(uri)
        assertNotNull(action)
        assertIs<SynapseAction.Download>(action)
        assertEquals("https://artstation.com/artwork/abc", action.url)
    }

    @Test
    fun `parses json payload actions`() {
        val jsonDownload = """{"action":"download","url":"https://twitter.com/user/status/123","dest":"D:\\Art","silent":false}"""
        val action = SynapseUriParser.parse(jsonDownload)
        assertNotNull(action)
        assertIs<SynapseAction.Download>(action)
        assertEquals("https://twitter.com/user/status/123", action.url)
        assertEquals("D:\\Art", action.destination)
        assertEquals(false, action.silent)
    }

    @Test
    fun `returns null on invalid or blank inputs`() {
        assertNull(SynapseUriParser.parse(""))
        assertNull(SynapseUriParser.parse("   "))
        assertNull(SynapseUriParser.parse(null))
        assertNull(SynapseUriParser.parse("random-invalid-string"))
    }
}
