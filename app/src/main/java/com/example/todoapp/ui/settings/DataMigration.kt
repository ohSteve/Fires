package com.example.todoapp.ui.settings

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.todoapp.data.AppDatabase
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DataMigration {

    fun exportDatabase(context: Context): Boolean {
        return try {
            val dbPath = context.getDatabasePath(com.example.todoapp.data.AppConstants.DATABASE_NAME)
            if (!dbPath.exists()) {
                Toast.makeText(context, "没有可导出的数据", Toast.LENGTH_SHORT).show()
                return false
            }
            val exportFile = File(context.cacheDir, "goal_tracker_database.sqlite")
            dbPath.copyTo(exportFile, overwrite = true)

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", exportFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "导出数据库 (SQLite格式-换手机导入用)"))
            true
        } catch (e: Exception) {
            Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_LONG).show()
            false
        }
    }

    fun exportTSV(context: Context): Boolean {
        return try {
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val exportDir = File(context.cacheDir, "tsv_$dateStr")
            exportDir.mkdirs()

            val dbPath = context.getDatabasePath(com.example.todoapp.data.AppConstants.DATABASE_NAME).absolutePath
            val db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY)

            val tables = listOf(
                "goals" to listOf("id","name","ddl","implementationPath","isAchieved","achievedAt","createdAt"),
                "milestones" to listOf("id","goalId","date","completedTaskName","completedTaskDetail","timeSpent","difficulties","futurePlans","createdAt"),
                "todo_items" to listOf("id","content","ddl","isCompleted","completedAt","createdAt"),
                "journals" to listOf("date","notes","entertainment","instantGratification","usageMinutes","usageTopApps","usageUpdateTime","createdAt"),
                "checkin_items" to listOf("id","name","requirements","startDate","endDate","cycleType","cycleConfig","isPaused","createdAt"),
                "checkin_records" to listOf("id","itemId","date","status","isBackfill","updatedAt")
            )

            for ((table, headers) in tables) {
                val file = File(exportDir, "$table.tsv")
                BufferedWriter(OutputStreamWriter(FileOutputStream(file), Charsets.UTF_8)).use { w ->
                    w.write(headers.joinToString("\t")); w.newLine()
                    db.rawQuery("SELECT * FROM $table", null).use { c ->
                        val n = c.columnCount
                        while (c.moveToNext()) {
                            val values = (0 until n).map { i ->
                                if (c.getType(i) == android.database.Cursor.FIELD_TYPE_NULL) ""
                                else c.getString(i) ?: ""
                            }
                            w.write(values.joinToString("\t")); w.newLine()
                        }
                    }
                }
            }
            db.close()

            // Zip
            val zipFile = File(context.cacheDir, "整点薯条_数据导出_$dateStr.zip")
            ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
                exportDir.listFiles()?.forEach { f ->
                    zip.putNextEntry(ZipEntry(f.name))
                    f.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
            exportDir.deleteRecursively()

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zipFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "导出数据 (TSV格式-数据分析用)"))
            true
        } catch (e: Exception) {
            Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_LONG).show()
            false
        }
    }

    fun importDatabase(context: Context, sourceUri: Uri): Boolean {
        return try {
            AppDatabase.closeInstance()
            val dbPath = context.getDatabasePath(com.example.todoapp.data.AppConstants.DATABASE_NAME)
            dbPath.parentFile?.mkdirs()

            // Backup before overwrite
            val backupFile = File(dbPath.parent, "${com.example.todoapp.data.AppConstants.DATABASE_NAME}.backup")
            if (dbPath.exists()) dbPath.copyTo(backupFile, overwrite = true)

            try {
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(dbPath).use { output -> input.copyTo(output) }
                }
                // Validate
                val testDb = SQLiteDatabase.openDatabase(dbPath.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
                testDb.rawQuery("SELECT count(*) FROM sqlite_master", null).use { c ->
                    if (!c.moveToFirst() || c.getInt(0) == 0) throw Exception("无效的数据库文件")
                }
                testDb.close()
                backupFile.delete()
                Toast.makeText(context, "导入成功，请重新进入APP", Toast.LENGTH_LONG).show()
                true
            } catch (e: Exception) {
                if (backupFile.exists()) { backupFile.copyTo(dbPath, overwrite = true); backupFile.delete() }
                throw e
            }
        } catch (e: Exception) {
            Toast.makeText(context, "导入失败: ${e.message}", Toast.LENGTH_LONG).show()
            false
        }
    }
}
