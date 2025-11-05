package com.bookscanner.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bookscanner.app.databinding.ItemBookBinding
import com.bookscanner.app.model.Book

/**
 * 书籍列表适配器
 */
class BookAdapter(
    private val onDeleteClick: (Book) -> Unit
) : ListAdapter<Book, BookAdapter.BookViewHolder>(BookDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BookViewHolder(binding, onDeleteClick)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder
     */
    class BookViewHolder(
        private val binding: ItemBookBinding,
        private val onDeleteClick: (Book) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(book: Book) {
            binding.tvBookTitle.text = book.title
            binding.tvBookDetail.text = book.getDetailInfo()
            binding.tvScannedTime.text = book.getFormattedTime()
            binding.btnDelete.setOnClickListener {
                onDeleteClick(book)
            }
        }
    }

    /**
     * DiffUtil回调，用于高效更新列表
     */
    private class BookDiffCallback : DiffUtil.ItemCallback<Book>() {
        override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean {
            return oldItem.scannedTime == newItem.scannedTime
        }

        override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean {
            return oldItem == newItem
        }
    }
}

