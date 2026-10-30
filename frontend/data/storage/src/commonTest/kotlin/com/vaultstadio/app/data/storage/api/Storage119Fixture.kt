package com.vaultstadio.app.data.storage.api

import com.vaultstadio.app.data.network.ApiClientConfig
import com.vaultstadio.app.data.network.HttpClientFactory
import com.vaultstadio.app.data.network.TokenProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine

internal const val STORAGE119_ITEM = """
{
  "success":true,
  "data":{
    "id":"file119",
    "name":"é.txt",
    "path":"/é.txt",
    "type":"FILE",
    "parentId":null,
    "size":4,
    "mimeType":"text/plain",
    "visibility":"PRIVATE",
    "isStarred":false,
    "isTrashed":false,
    "createdAt":"2026-10-27T12:00:00Z",
    "updatedAt":"2026-10-27T12:00:00Z"
  }
}
"""
internal const val STORAGE119_LIST = """{"success":true,"data":[]} """
internal const val STORAGE119_PAGE = """
{
  "success":true,
  "data":{
    "items":[],
    "total":9007199254740993,
    "page":2,
    "pageSize":50,
    "totalPages":180143985094820,
    "hasMore":true
  }
}
"""
internal const val STORAGE119_BATCH = """
{
  "success":true,
  "data":{
    "successful":1,
    "failed":1,
    "errors":[
      {
        "itemId":"denied119",
        "error":"No grant"
      }
    ]
  }
}
"""
internal const val STORAGE119_CHUNK = """
{
  "success":true,
  "data":{
    "uploadId":"upload119",
    "fileName":"é.txt",
    "totalSize":4,
    "uploadedBytes":2,
    "progress":0.5,
    "receivedChunks":[
      0
    ],
    "missingChunks":[
      1
    ],
    "isComplete":false
  }
}
"""
internal const val STORAGE119_UNIT = """{"success":true}"""
internal suspend fun <T> withStorage119Client(engine: MockEngine, block: suspend (HttpClient) -> T): T {
    val client = HttpClientFactory.create(
        ApiClientConfig("https://audit.invalid"),
        TokenProvider {
            "ephemeral119"
        },
        engine,
    )
    try {
        return block(client)
    } finally {
        client.close()
        engine.close()
    }
}
