package goliot.fr.networkconfig

import android.content.Context
import android.content.SharedPreferences
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import kotlinx.android.synthetic.main.fragment_settings.*
import org.json.JSONException
import org.json.JSONObject

class SettingsFragment : Fragment() {

    val TAG: String = "SettingsFragment"
    val arrayApps: Array<String> = arrayOf("goliot.fr.networkconfigappdata")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        val saveSettingsBtn = view.findViewById(R.id.saveSettingsBtn) as Button
        saveSettingsBtn.setOnClickListener { v -> saveSettings() }

        return view

    }

    fun setData(data: String) {
        text_data.text = data
    }

    private fun getJsonData(): JSONObject? {
        var json: JSONObject? = null
        try {
            json = JSONObject(text_data.text.toString())
        } catch (ex: JSONException) {
            Toast.makeText(context, "le contenu n'est pas un json valide !", Toast.LENGTH_SHORT).show()
        }
        return json
    }

    private fun saveSettings() {
        val json: JSONObject? = getJsonData()
        if (json != null) {
            val data: JSONObject? = json.optJSONObject("data")
            if (data != null) {
                shareData(data)
            }
            val networkInfo: JSONObject? = json.optJSONObject("network")
            if (networkInfo != null) {
                val ssid: String = networkInfo.optString("ssid")
                val pass: String = networkInfo.optString("pass")
                if (ssid.isNotEmpty() && pass.isNotEmpty()) {
                    connectToWPAWiFi(ssid, pass)
                }
            }

        }
    }

    private fun shareData(data: JSONObject) {
        arrayApps.forEach {
            val mContext: Context = context!!.createPackageContext(
                    it,
                    Context.MODE_PRIVATE)

            val filePreferences: String = data.optString("preferences", "DefaultPref")
            val pref: SharedPreferences = mContext.getSharedPreferences(filePreferences, Context.MODE_PRIVATE)
            val editor: SharedPreferences.Editor = pref.edit()
            for (key: String in data.keys()) {
                editor.putString(key, data.getString(key))
            }
            editor.apply()
        }
    }


    //connects to the given ssid
    fun connectToWPAWiFi(ssid: String, pass: String) {
        val wm: WifiManager = context!!.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (!wm.isWifiEnabled)
            wm.isWifiEnabled = true

        if (isConnectedTo(ssid)) { //see if we are already connected to the given ssid
            Toast.makeText(context, "Connected to $ssid", Toast.LENGTH_SHORT).show()
            return
        }
        var wifiConfig = getWiFiConfig(ssid)
        if (wifiConfig == null) {
            //if the given ssid is not present in the WiFiConfig, create a config for it
            createWPAProfile(ssid, pass)
            wifiConfig = getWiFiConfig(ssid)
        }
        wm.disconnect()
        wm.enableNetwork(wifiConfig!!.networkId, true)
        wm.reconnect()
        Log.d(TAG, "intiated connection to SSID $ssid")
    }

    fun isConnectedTo(ssid: String): Boolean {
        val wm: WifiManager = context!!.getSystemService(Context.WIFI_SERVICE) as WifiManager
        // double quote in String value
        val wifiSsid = wm.connectionInfo.ssid.replace("\"", "")
        if (wifiSsid == ssid) {
            return true
        }
        return false
    }

    fun getWiFiConfig(ssid: String): WifiConfiguration? {
        val wm: WifiManager = context!!.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiList = wm.configuredNetworks
        for (item in wifiList) {
            if (item.SSID != null) {
                val wifiSsid: String = item.SSID.replace("\"", "")
                if (wifiSsid == ssid) {
                    return item
                }
            }
        }
        return null
    }

    fun createWPAProfile(ssid: String, pass: String) {
        Log.d(TAG, "Saving SSID : $ssid")
        val conf = WifiConfiguration()
        conf.SSID = "\"$ssid\""
        conf.preSharedKey = "\"$pass\""
        conf.hiddenSSID = true
        val wm: WifiManager = context!!.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wm.addNetwork(conf)
        Log.d(TAG, "saved SSID to WiFiManger")
    }
}
