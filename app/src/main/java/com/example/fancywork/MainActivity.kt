package com.example.fancywork

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

// obviously we haven't done anything yet
class MainActivity : AppCompatActivity() {
    lateinit var colors: List<Pair<String, Triple<Int, Int, Int>>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Reading thread colors dictionary from resources.
        colors = PixelizationAlgorithm.getThreadColors(resources)
        drawImage(R.drawable.shiba)
    }

    fun drawImage(img: Int) {
        val image = findViewById<ImageView>(R.id.imageView)
        val source = BitmapFactory.decodeResource(resources, img)
        val bitmap = PixelizationAlgorithm.getPixelsFromImage(
            source, 10, 5, colors
        ).first
        image.setImageBitmap(bitmap)
        image.layoutParams.width = bitmap.width
        image.layoutParams.height = bitmap.height
    }

    // todo for butten download image
    public fun download(view: View) {
        drawImage(R.drawable.icon)
    }

    // todo for button open scheme
    public fun open(view: View) {
        drawImage(R.drawable.bleach)
    }
}
