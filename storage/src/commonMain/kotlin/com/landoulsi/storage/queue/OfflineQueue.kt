package com.landoulsi.storage.queue

class OfflineQueue(private val database: LandoulsiDatabase) {
    private val queries = database.pingQueueQueries

    fun enqueue(payload: String, createdAtMillis: Long) {
        queries.insertPing(payload, createdAtMillis)
    }

    fun peekAllInOrder(): List<PingQueueEntry> = queries.selectAllOrdered().executeAsList()

    fun remove(id: Long) {
        queries.deleteById(id)
    }

    fun clear() {
        queries.deleteAll()
    }

    fun size(): Long = queries.countAll().executeAsOne()

    /**
     * Drains the queue in FIFO order, calling [send] for each entry. An entry is only removed
     * once [send] returns true; the first false stops the flush so everything from that point
     * stays queued, in order, for the next reconnect attempt.
     */
    suspend fun flush(send: suspend (String) -> Boolean): Int {
        var sentCount = 0
        for (entry in peekAllInOrder()) {
            if (!send(entry.payload)) break
            remove(entry.id)
            sentCount++
        }
        return sentCount
    }
}
