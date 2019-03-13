package ozlemdogan.translatesmsapp

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL


private const val SMS_READ_REQUEST_CODE = 1001
class ContactRecyclerViewAdapter(private val context: Context,private val callingActivity: Activity, private val contactList: ArrayList<Contact>) : RecyclerView.Adapter<ContactRecyclerViewAdapter.ViewHolder>() {
    private var phoneNumber: String =""
    private var originalMesseage:String=""

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText = view.findViewById<TextView>(R.id.tvMessageBody)
        val phoneText = view.findViewById<TextView>(R.id.tvPhNumber)
        val sendSmsBtn = view.findViewById<Button>(R.id.btnSend)
    }

    override fun onCreateViewHolder(parent: ViewGroup, p1: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.recylerview_item, parent, false))
    }

    override fun getItemCount(): Int {
        return contactList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.messageText.text = contactList[position].message
        holder.phoneText.text = contactList[position].phone

        holder.sendSmsBtn.setOnClickListener {
            phoneNumber=contactList[position].phone
            originalMesseage=contactList[position].message
           val sharedPreferences:SharedPreferences=PreferenceManager.getDefaultSharedPreferences(context)
           val sourceLanguageCode = sharedPreferences?.getString("source", "tr")!!
           val targetLanguageCode = sharedPreferences?.getString("target", "en")!!
            val languagePair=sourceLanguageCode+"-"+targetLanguageCode
            Translate(contactList[position].message,languagePair)

        }
    }

    private fun sendSMS(originalMessage: String, translatedMessage: String,phoneNumber:String) {
        val alertDialog = AlertDialog.Builder(context)
        alertDialog.setTitle("Do you confirm to send SMS?")
        alertDialog.setMessage("From: $phoneNumber\n\nTo: $phoneNumber\n\nOriginal Message: $originalMessage\n\nTranslated Message: $translatedMessage")
        alertDialog.setCancelable(false)
        alertDialog.setPositiveButton("Yes") { dialog, _ ->
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:$phoneNumber"))
                intent.putExtra("sms_body", translatedMessage)
                context.startActivity(intent)
            } else {
                ActivityCompat.requestPermissions(callingActivity, arrayOf(Manifest.permission.SEND_SMS), SMS_READ_REQUEST_CODE)
            }
            dialog.dismiss()
        }
        alertDialog.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        alertDialog.show()
    }


    internal fun Translate(textToBeTranslated: String, languagePair: String) {
        val translatorBackgroundTask = TranslatorBackgroundTask(context)

        var translationResult = ""

        translationResult = translatorBackgroundTask.execute(textToBeTranslated, languagePair).toString() // Returns the translated text as a String
        Log.d("Translation Result", translationResult) // Logs the result in Android Monitor

    }
    inner class


    TranslatorBackgroundTask//Set Context
    internal constructor(//Declare Context
            internal var ctx: Context) : AsyncTask<String, Void, String>() {

        internal lateinit var resultString: String

        override fun doInBackground(vararg params: String): String? {
            //String variables
            val textToBeTranslated = params[0]
            val languagePair = params[1]

            var jsonString: String

            try {
                //Set up the translation call URL
                val yandexKey = "trnsl.1.1.20190121T161147Z.15a1c13d43930d78.0b7dda80db6e2bfa5eca47ead87bb1bfee1362d7"
                val yandexUrl = ("https://translate.yandex.net/api/v1.5/tr.json/translate?key=" + yandexKey
                        + "&text=" + textToBeTranslated + "&lang=" + languagePair)
                val yandexTranslateURL = URL(yandexUrl)

                //Set Http Conncection, Input Stream, and Buffered Reader
                val httpJsonConnection = yandexTranslateURL.openConnection() as HttpURLConnection
                val inputStream = httpJsonConnection.inputStream
                val bufferedReader = BufferedReader(InputStreamReader(inputStream))

                //Set string builder and insert retrieved JSON result into it
                val jsonStringBuilder = StringBuilder()
                var jsonString :String?
                do{
                    jsonString = bufferedReader.readLine()
                    if(jsonString==null)
                        break
                    jsonStringBuilder.append(jsonString + "\n")
                }while(true)


                //Close and disconnect
                try {
                    bufferedReader.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                inputStream.close()
                httpJsonConnection.disconnect()

                //Making result human readable
                resultString = jsonStringBuilder.toString().trim { it <= ' ' }

                //Getting the characters between [ and ]

                resultString = resultString.substring(resultString.indexOf('[') + 1)
                resultString = resultString.substring(0, resultString.indexOf("]"))

                //Getting the characters between " and "
                resultString = resultString.substring(resultString.indexOf("\"") + 1)
                resultString = resultString.substring(0, resultString.indexOf("\""))

                Log.d("Translation Result:", resultString)
                return jsonStringBuilder.toString().trim { it <= ' ' }

            } catch (e: MalformedURLException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return null
        }
        //String text = String.valueOf(resultString);

        override fun onPreExecute() {
            super.onPreExecute()
        }

        override fun onPostExecute(result: String) {
            sendSMS(originalMesseage,resultString,phoneNumber)
           // Toast.makeText(ctx, resultString, Toast.LENGTH_LONG).show()
            super.onPostExecute(result)
        }

        override fun onProgressUpdate(vararg values: Void) {
            super.onProgressUpdate(*values)
        }
    }
}