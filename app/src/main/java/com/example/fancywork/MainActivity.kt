package com.example.fancywork

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

// obviously we haven't done anything yet
class MainActivity : AppCompatActivity() {
    lateinit var colors: List<Pair<String, Triple<Int, Int, Int>>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Reading thread colors dictionary from resources.
        colors = PixelizationAlgorithm.getThreadColors(resources)
        val image = findViewById<ImageView>(R.id.imageView)
        val bitmap = PixelizationAlgorithm.getPixelsFromImage(
            BitmapFactory.decodeResource(resources, R.drawable.bleach), 10, 5, colors
        ).first
        image.setImageBitmap(bitmap)
        image.layoutParams.width = bitmap.width
        image.layoutParams.height = bitmap.height
    }

    // todo for butten download image
    public fun download(view: View) {
        Toast.makeText(
            view.context,
            "вы ткнули на загрузку!",
            Toast.LENGTH_LONG
        ).show()
    }

    // todo for button open scheme
    public fun open(view: View) {
        Toast.makeText(
            view.context,
            "вы ткнули на открытие!",
            Toast.LENGTH_LONG
        ).show()
    }
}
