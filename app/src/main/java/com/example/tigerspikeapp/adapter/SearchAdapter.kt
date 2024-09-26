package com.example.tigerspikeapp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tigerspikeapp.R
import com.google.android.gms.maps.model.LatLng

/**
 * RecyclerView Adapter for save search items data
 * */
class SearchAdapter : RecyclerView.Adapter<SearchAdapter.ItemHolder> {
    companion object {
        lateinit var clickListener: ClickListener
    }

    interface ClickListener {
        fun onItemClick(position: Int, v: View)
    }

    fun setOnItemClickListener(clickListener: ClickListener) {
        SearchAdapter.clickListener = clickListener
    }

    var context: Context
    var searchList: ArrayList<SearchModel>

    constructor(context: Context, searchList: ArrayList<SearchModel>) : super() {
        this.context = context
        this.searchList = searchList
    }

    fun reloadList(searchList: ArrayList<SearchModel>) {
        this.searchList = searchList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
        var v: View = LayoutInflater.from(parent.context).inflate(R.layout.item_search, parent, false)
        return ItemHolder(v)
    }

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
        var item = searchList[holder.adapterPosition]
        holder.tvUsername.text = item.username
        holder.tvContent.text = item.content
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getItemCount(): Int {
        return searchList.size
    }

    class ItemHolder : RecyclerView.ViewHolder, View.OnClickListener {

        var tvUsername: TextView
        var tvContent: TextView

        constructor(itemView: View) : super(itemView) {
            tvUsername = itemView.findViewById(R.id.tvUsername)
            tvContent = itemView.findViewById(R.id.tvContent)
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            clickListener.onItemClick(adapterPosition, v)

        }
    }

    /**
     * Save search research model item
     * */
    class SearchModel {
        var username = ""
        var content = ""
        var latLng: LatLng? = null
    }
}