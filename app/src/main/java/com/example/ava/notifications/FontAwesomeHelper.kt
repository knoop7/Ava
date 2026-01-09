package com.example.ava.notifications

import android.content.Context
import android.graphics.Typeface


object FontAwesomeHelper {
    private var typeface: Typeface? = null
    
    
    fun loadFont(context: Context): Typeface {
        if (typeface == null) {
            typeface = Typeface.createFromAsset(context.assets, "fonts/fa-solid-900.ttf")
        }
        return typeface!!
    }
    
    
    fun getIconChar(iconClass: String): String {
        return when (iconClass) {
            
            "fa-sun" -> "\uf185"
            "fa-moon" -> "\uf186"
            "fa-cloud-sun" -> "\uf6c4"
            "fa-cloud" -> "\uf0c2"
            "fa-cloud-rain" -> "\uf73d"
            "fa-cloud-bolt" -> "\uf76c"  
            "fa-snowflake" -> "\uf2dc"
            "fa-wind" -> "\uf72e"
            "fa-smog" -> "\uf75f"  
            "fa-temperature-high" -> "\uf769"
            "fa-temperature-low" -> "\uf76b"
            "fa-temperature-arrow-up" -> "\uf0a8"  

            
            "fa-bell" -> "\uf0f3"
            "fa-bell-slash" -> "\uf1f6"
            "fa-door-open" -> "\uf52b"
            "fa-door-closed" -> "\uf52a"
            "fa-lock" -> "\uf023"
            "fa-lock-open" -> "\uf3c1"
            "fa-shield-halved" -> "\uf3ed"
            "fa-shield" -> "\uf132"
            "fa-eye" -> "\uf06e"
            "fa-video" -> "\uf03d"
            
            
            "fa-droplet" -> "\uf043"
            "fa-fire" -> "\uf06d"
            "fa-fire-flame-curved" -> "\uf7e4"
            "fa-triangle-exclamation" -> "\uf071"
            "fa-skull-crossbones" -> "\uf714"
            "fa-radiation" -> "\uf7b9"
            "fa-biohazard" -> "\uf780"
            
            
            "fa-tv" -> "\uf26c"
            "fa-plug" -> "\uf1e6"
            "fa-plug-circle-xmark" -> "\ue55e"  
            "fa-bolt" -> "\uf0e7"
            "fa-lightbulb" -> "\uf0eb"
            "fa-fan" -> "\uf863"
            "fa-air-freshener" -> "\uf5d0"
            "fa-robot" -> "\uf544"
            "fa-blender" -> "\uf517"
            "fa-mug-hot" -> "\uf7b6"
            "fa-shower" -> "\uf2cc"
            "fa-faucet" -> "\ue005"
            "fa-faucet-drip" -> "\ue006"  
            "fa-hot-tub-person" -> "\uf593"  
            "fa-print" -> "\uf02f"
            "fa-server" -> "\uf233"
            "fa-hard-drive" -> "\uf0a0"  
            "fa-wifi" -> "\uf1eb"
            
            
            "fa-car" -> "\uf1b9"
            "fa-car-side" -> "\uf5e4"
            "fa-charging-station" -> "\uf5e7"
            "fa-battery-full" -> "\uf240"
            "fa-battery-half" -> "\uf242"
            "fa-battery-quarter" -> "\uf243"
            "fa-battery-empty" -> "\uf244"
            "fa-gas-pump" -> "\uf52f"
            
            
            "fa-box" -> "\uf466"
            "fa-bowl-rice" -> "\ue3e6"  
            "fa-box-open" -> "\uf49e"
            "fa-envelope" -> "\uf0e0"
            "fa-envelope-open" -> "\uf2b6"
            "fa-mailbox" -> "\uf813"
            
            
            "fa-user" -> "\uf007"
            "fa-users" -> "\uf0c0"
            "fa-baby" -> "\uf77c"
            "fa-person-walking" -> "\uf554"
            "fa-person-running" -> "\uf70c"
            "fa-dumbbell" -> "\uf44b"
            "fa-bed" -> "\uf236"
            
            
            "fa-dog" -> "\uf6d3"
            "fa-cat" -> "\uf6be"
            "fa-paw" -> "\uf1b0"
            "fa-horse" -> "\uf6f0"
            
            
            "fa-clock" -> "\uf017"
            "fa-alarm-clock" -> "\uf34e"
            "fa-calendar" -> "\uf133"
            "fa-calendar-check" -> "\uf274"
            "fa-hourglass" -> "\uf254"
            
            
            "fa-champagne-glasses" -> "\uf79f"
            "fa-gift" -> "\uf06b"
            "fa-cake-candles" -> "\uf1fd"
            "fa-tree" -> "\uf1bb"
            "fa-dragon" -> "\uf6d5"
            "fa-star" -> "\uf005"
            "fa-heart" -> "\uf004"
            "fa-music" -> "\uf001"
            "fa-party-horn" -> "\ue31c"
            
            
            "fa-hurricane" -> "\uf751"
            "fa-tornado" -> "\uf76f"
            "fa-house-crack" -> "\ue3b1"
            "fa-earth-asia" -> "\uf57e"
            "fa-house-chimney-window" -> "\ue0bd"  
            "fa-warehouse" -> "\uf494"  
            
            
            "fa-leaf" -> "\uf06c"
            "fa-seedling" -> "\uf4d8"
            "fa-power-off" -> "\uf011"
            "fa-gear" -> "\uf013"
            "fa-sliders" -> "\uf1de"
            
            
            "fa-shirt" -> "\uf553"
            
            
            "fa-blinds" -> "\uf8fb"
            "fa-blinds-raised" -> "\uf8fc"
            
            
            "fa-briefcase" -> "\uf0b1"
            "fa-handshake" -> "\uf2b5"
            
            
            "fa-film" -> "\uf008"
            "fa-clapperboard" -> "\ue131"
            "fa-tower-broadcast" -> "\uf519"  
            
            
            "fa-volume-high" -> "\uf028"
            "fa-volume-low" -> "\uf027"
            "fa-volume-xmark" -> "\uf6a9"
            "fa-microphone" -> "\uf130"  

            
            "fa-book-open" -> "\uf5db"

            
            "fa-gamepad" -> "\uf11b"

            
            "fa-trash-can" -> "\uf2ed"
            "fa-glass-water" -> "\ue4e5"

            
            "fa-weight-scale" -> "\uf496"
            "fa-pills" -> "\uf484"

            
            "fa-sink" -> "\ue06d"
            "fa-utensils" -> "\uf2e7"
            "fa-kitchen-set" -> "\ue651"

            
            "fa-person-swimming" -> "\uf5c4"

            
            else -> "\uf0f3"  
        }
    }
}
