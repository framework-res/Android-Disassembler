package com.kyhsgeekcode.filechooser

import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.kyhsgeekcode.disassembler.databinding.NewFileChooserRowBinding
import com.kyhsgeekcode.disassembler.showEditDialog
import com.kyhsgeekcode.filechooser.model.FileItem
import kotlinx.coroutines.*
import splitties.init.appCtx
import java.util.*
import kotlin.collections.ArrayList

class NewFileChooserAdapter(
    private
    val parentActivity: NewFileChooserActivity
) : RecyclerView.Adapter<NewFileChooserAdapter.ViewHolder>() {
    val TAG = "Adapter"
    private val values: MutableList<FileItem> = ArrayList()
    val onClickListener: View.OnClickListener
    val onLongClickListener: View.OnLongClickListener
    val backStack = Stack<FileItem>()
    var currentParentItem: FileItem = FileItem.rootItem

    init {
        backStack.clear()
        // backStack.push(FileItem.rootItem)
        currentParentItem = FileItem.rootItem
        onClickListener = View.OnClickListener { v ->
            val item = v.tag as FileItem
            if (!item.isAccessible()) {
                Toast.makeText(parentActivity, "the file is inaccessible", Toast.LENGTH_SHORT)
                    .show()
                return@OnClickListener
            }
            if (item == FileItem.others) {
                parentActivity.showOtherChooser()
                return@OnClickListener
            }
            if (item == FileItem.zoo) {
                parentActivity.showZoo()
                return@OnClickListener
            }
            if (item == FileItem.hash) {
                val editText = EditText(parentActivity)
                showEditDialog(
                    parentActivity,
                    "Search from infosec",
                    "Enter hash",
                    editText,
                    appCtx.getString(android.R.string.ok),
                    DialogInterface.OnClickListener { dialog, which ->
                        val hash = editText.text.toString()
                        if (hash.isEmpty()) {
                            Toast.makeText(
                                parentActivity,
                                "Please enter a valid hash",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@OnClickListener
                        }
                        parentActivity.showHashSite(hash)
                    },
                    null,
                    null
                )
                // https://infosec.cert-pa.it/analyze/7b3491e0028d443f11989efaeb0fbec2.html
                return@OnClickListener
            }
            if (item.canExpand()) {
                // 물어본다.
                if (!item.isProjectAble() && !item.isRawAvailable()) {
                    navigateInto(item)
                    return@OnClickListener
                }
                Toast.makeText(
                    parentActivity,
                    "Long press to see advanced options",
                    Toast.LENGTH_SHORT
                ).show()
                navigateInto(item)
//                AlertDialog.Builder(parentActivity)
//                        .setTitle("Choose Action")
//                        .also {
//                            if (item.isProjectAble()) {
//                                it.setPositiveButton("Open as project") { _: DialogInterface, _: Int ->
//                                    parentActivity.openAsProject(item)
//                                }
//                            }
//                        }.also {
//                            if (item.isRawAvailable()) {
//                                it.setNeutralButton("Open raw") { _, _ ->
//                                    parentActivity.openRaw(item)
//                                }
//                            }
//                        }
//                        .setNegativeButton("Navigate into") { _, _ ->
//
//                        }.show()
            } else {
                // 물어보고 진행한다.
                AlertDialog.Builder(parentActivity)
                    .setTitle("Open the file ${item.text}?")
                    .setPositiveButton("Open") { _, _ ->
                        parentActivity.openRaw(item)
                    }.setNegativeButton("No") { dialog, _ ->
                        dialog.cancel()
                    }.show()
            }
        }

        onLongClickListener = View.OnLongClickListener { v ->
            val item = v.tag as FileItem
            if (!item.isAccessible()) {
                Toast.makeText(parentActivity, "the file is inaccessible", Toast.LENGTH_SHORT)
                    .show()
                return@OnLongClickListener true
            }
            if (item.canExpand()) {
                // 물어본다.
                if (!item.isProjectAble() && !item.isRawAvailable()) {
                    Toast.makeText(
                        parentActivity,
                        "The file/directory cannot be opened as project",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@OnLongClickListener true
                }
                AlertDialog.Builder(parentActivity)
                    .setTitle("Choose Action")
                    .also {
                        if (item.isProjectAble()) {
                            it.setPositiveButton("Open as project") { _: DialogInterface, _: Int ->
                                parentActivity.openAsProject(item)
                            }
                        }
                    }.also {
                        if (item.isRawAvailable()) {
                            it.setNeutralButton("Open raw") { _, _ ->
                                parentActivity.openRaw(item)
                            }
                        }
                    }.show()
            } else {
                // 물어보고 진행한다.
                AlertDialog.Builder(parentActivity)
                    .setTitle("Open the file ${item.text}?")
                    .setPositiveButton("Open") { _, _ ->
                        parentActivity.openRaw(item)
                    }.setNegativeButton("No") { dialog, _ ->
                        dialog.cancel()
                    }.show()

            }
            true
        }
//        viewModelScope.launch {
//            tryAddRootItems()
//        }
    }

    suspend fun tryAddRootItems() {
        val items: List<FileItem> = try {
            FileItem.rootItem.listSubItems()
        } catch (e: Exception) {
            arrayListOf(FileItem(e.message ?: ""))
        }
        values.addAll(items)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = NewFileChooserRowBinding.bind(view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Log.d(TAG,"onCreateViewHolder")
        val binding =
            NewFileChooserRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
//        listView = parent as RecyclerView
        return ViewHolder(binding.root)
    }

    override fun getItemCount(): Int = values.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values[position]
        with(holder.itemView) {
            tag = item
            setOnClickListener(onClickListener)
            setOnLongClickListener(onLongClickListener)
        }
        with(holder.binding.textViewNewItemName) {
            text = item.text
        }
        holder.binding.imageViewFileIcon.setImageDrawable(item.drawable)
    }

    private fun navigateInto(item: FileItem) {
        CoroutineScope(Dispatchers.Default).launch {
            val subItems = listSubItems(item)
            if (!subItems.any { it.file?.isFile != false }) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        parentActivity,
                        "Please try granting file permissions in the settings," +
                                " if you cannot see any files after Android 30" +
                                " even if there really exists files!",
                        Toast.LENGTH_LONG
                    )
                        .show()
                }
            }
            if (subItems.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        parentActivity,
                        "The item has no children",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }
            addItemsToListSorted(subItems)
            backStack.push(currentParentItem)
            currentParentItem = item
            withContext(Dispatchers.Main) {
                notifyDataSetChanged()
            }
        }
    }

    private fun addItemsToListSorted(subItems: List<FileItem>) {
        values.clear()
        values.addAll(subItems)
        values.sortWith(
            compareBy(
                { !it.text.endsWith("/") },
                { it.text[0].lowercaseChar() },
                { it.text })
        )
    }

    fun onBackPressedShouldFinish(): Boolean {
        if (backStack.empty()) return true
        val lastItem = backStack.pop()
        currentParentItem = lastItem
        CoroutineScope(Dispatchers.Default).launch {
            val items = listSubItemsCached(currentParentItem)
            addItemsToListSorted(items)
            withContext(Dispatchers.Main) {
                notifyDataSetChanged()
            }
        }
        return false
    }

    private suspend fun listSubItems(item: FileItem): List<FileItem> {
        var showedTotal = false
        return withContext(Dispatchers.IO) {
            try {
                val ret = item.listSubItems { current, total ->
                    CoroutineScope(Dispatchers.Main).launch {
                        parentActivity.publishProgress(current, if (showedTotal) null else total)
                        showedTotal = true
                    }
                }
                withContext(Dispatchers.Main) {
                    parentActivity.finishProgress()
                }
                ret
            } catch (e: Exception) {
                arrayListOf(FileItem(e.message ?: ""))
            }
        }
    }

    private suspend fun listSubItemsCached(item: FileItem): List<FileItem> {
        return listSubItems(item) // item.cachedSubItems() ?:
    }
}
