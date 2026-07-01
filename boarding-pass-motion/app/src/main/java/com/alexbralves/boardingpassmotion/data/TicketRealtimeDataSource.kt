package com.alexbralves.boardingpassmotion.data

import com.alexbralves.boardingpassmotion.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class TicketScanRow(
  val code: String,
  @SerialName("scan_count") val scanCount: Int = 0,
)

class TicketRealtimeDataSource {
  private val enabled =
    BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

  private val client by lazy {
    createSupabaseClient(
      supabaseUrl = BuildConfig.SUPABASE_URL,
      supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
    ) {
      install(Postgrest)
      install(Realtime)
    }
  }

  fun scanCountChanges(code: String): Flow<Int> {
    if (!enabled) return emptyFlow()

    return callbackFlow {
      val channel = client.channel("ticket-validation-$code")
      val initialLoad =
        launch {
          runCatching {
            client
              .from("tickets")
              .select {
                filter {
                  eq("code", code)
                }
              }
              .decodeSingle<TicketScanRow>()
              .scanCount
          }.onSuccess { scanCount ->
            trySend(scanCount)
          }
        }
      val updates =
        channel
          .postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "tickets"
            filter("code", FilterOperator.EQ, code)
          }
          .mapNotNull { action ->
            val record =
              when (action) {
                is PostgresAction.Update -> action.record
                is PostgresAction.Insert -> action.record
                is PostgresAction.Select -> action.record
                is PostgresAction.Delete -> null
              }
            if (record?.get("code")?.jsonPrimitive?.contentOrNull != code) return@mapNotNull null
            (record["scan_count"] as? JsonPrimitive)
              ?.jsonPrimitive
              ?.intOrNull
          }
          .onEach { scanCount -> trySend(scanCount) }
          .launchIn(this)

      channel.subscribe()

      awaitClose {
        initialLoad.cancel()
        updates.cancel()
        runBlocking { channel.unsubscribe() }
        channel.realtime.disconnect()
      }
    }
  }
}
