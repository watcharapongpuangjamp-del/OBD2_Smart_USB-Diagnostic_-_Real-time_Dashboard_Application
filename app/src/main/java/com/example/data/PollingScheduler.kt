package com.example.data

import com.example.obd.OBD2Command
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue

class PollingScheduler(
    private val onExecuteCommand: suspend (OBD2Command) -> String
) {
    private val tasks = ConcurrentLinkedQueue<PollTask>()
    private var job: Job? = null

    fun addTasks(commands: List<OBD2Command>) {
        tasks.addAll(commands.map { PollTask(it) })
    }

    fun start(scope: CoroutineScope, onUpdate: (OBD2Command, String) -> Unit) {
        job = scope.launch(Dispatchers.IO) {
            while (isActive) {
                val now = System.currentTimeMillis()
                for (task in tasks) {
                    val interval = when (task.command.priority) {
                        OBD2Command.CommandPriority.HIGH -> 50L // 20Hz
                        OBD2Command.CommandPriority.MEDIUM -> 500L // 2Hz
                        OBD2Command.CommandPriority.LOW -> 5000L // 0.2Hz
                    }

                    if (now - task.lastRun >= interval) {
                        val response = onExecuteCommand(task.command)
                        task.lastRun = now
                        onUpdate(task.command, response)
                    }
                }
                delay(10) // Tick
            }
        }
    }

    fun stop() {
        job?.cancel()
    }

    private class PollTask(val command: OBD2Command, var lastRun: Long = 0L)
}
