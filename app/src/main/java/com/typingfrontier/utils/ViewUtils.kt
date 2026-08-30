package com.typingfrontier.utils

import android.app.AlertDialog
import android.content.Context
import android.graphics.ColorFilter
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.typingfrontier.R

object ViewUtils {

    fun showZoomDialog(
        context: Context,
        imageRes: Int,
        title: String,
        description: String? = null,
        colorFilter: ColorFilter? = null,
        alpha: Float = 1.0f
    ) {
        val builder = AlertDialog.Builder(context)
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.dialog_zoom, null)
        
        val imgZoom = dialogView.findViewById<ImageView>(R.id.imgZoom)
        val txtTitle = dialogView.findViewById<TextView>(R.id.txtZoomTitle)
        val txtDesc = dialogView.findViewById<TextView>(R.id.txtZoomDesc)
        
        imgZoom.setImageResource(imageRes)
        imgZoom.colorFilter = colorFilter
        imgZoom.alpha = alpha
        
        txtTitle.text = title
        if (description != null) {
            txtDesc.text = description
            txtDesc.visibility = View.VISIBLE
        } else {
            txtDesc.visibility = View.GONE
        }
        
        builder.setView(dialogView)
        builder.setPositiveButton("Fechar", null)
        
        val dialog = builder.create()
        dialog.show()
    }
}
