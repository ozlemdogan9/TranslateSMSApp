package ozlemdogan.translatesmsapp

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_home.*
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.AsyncTask
import android.preference.PreferenceManager
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.language_dialog_layout.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

private const val SMS_READ_REQUEST_CODE = 1001
private const val SMS_SEND_REQUEST_CODE = 1002
private var phoneNumber: String = ""
private var sourceLanguageCode = "tr"
private var targetLanguageCode = "en"
private var sharedPreferences: SharedPreferences? = null
private var editor: SharedPreferences.Editor? = null

class HomeActivity : AppCompatActivity() {

    internal var context: Context = this
    private lateinit var contactAdapter: ContactRecyclerViewAdapter
    private var contactList = ArrayList<Contact>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        sharedPreferences =PreferenceManager.getDefaultSharedPreferences(context)
        sourceLanguageCode = sharedPreferences?.getString("source", "tr")!!
        targetLanguageCode = sharedPreferences?.getString("target", "en")!!
        getSMS()

    }



    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.home_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        when (item?.itemId) {
            R.id.refresh_menu -> {
                getSMS()
            }
            R.id.sourceLang_menu -> {
                val selectSourceDialog = getLanguagesSelectDialog()
                selectSourceDialog.languages_lv.setOnItemClickListener { _, _, position, _ ->
                    sourceLanguageCode = selectSourceDialog.languages_lv.getItemAtPosition(position).toString().split("-")[1].trim()
                    editor = sharedPreferences?.edit()
                    editor?.putString("source", sourceLanguageCode)
                    editor?.apply()
                    selectSourceDialog.dismiss()
                }
                selectSourceDialog.show()
            }
            R.id.targetLang_menu -> {
                val selectTargetDialog = getLanguagesSelectDialog()
                selectTargetDialog.languages_lv.setOnItemClickListener { _, _, position, _ ->
                    targetLanguageCode = selectTargetDialog.languages_lv.getItemAtPosition(position).toString().split("-")[1].trim()
                    editor = sharedPreferences?.edit()
                    editor?.putString("target", targetLanguageCode)
                    editor?.apply()
                    selectTargetDialog.dismiss()
                }
                selectTargetDialog.show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getLanguagesSelectDialog(): Dialog {
        val dialog = Dialog(this@HomeActivity)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setContentView(R.layout.language_dialog_layout)
        return dialog
    }

    private fun getSMS() {
        if (contactList.isNotEmpty())
            contactList.clear()

        if (ContextCompat.checkSelfPermission(this@HomeActivity, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
            val contentResolver = contentResolver
            val smsInboxCursor = contentResolver.query(Uri.parse("content://sms/inbox"), null, null, null, null)
            val indexBody = smsInboxCursor!!.getColumnIndex("body")
            val indexAddress = smsInboxCursor.getColumnIndex("address")
            if (indexBody < 0 || !smsInboxCursor.moveToFirst())
                return
            do {
                val message = smsInboxCursor.getString(indexBody)
                val phone = smsInboxCursor.getString(indexAddress)
                contactList.add(Contact(message, phone))
            } while (smsInboxCursor.moveToNext())
            smsInboxCursor.close()
            showContacts()
        } else {
            ActivityCompat.requestPermissions(this@HomeActivity, arrayOf(Manifest.permission.READ_SMS), SMS_READ_REQUEST_CODE)
        }
    }

    private fun showContacts() {
        contactAdapter = ContactRecyclerViewAdapter(this@HomeActivity, this@HomeActivity, contactList)
        val linearLayoutManager = LinearLayoutManager(this@HomeActivity)
        linearLayoutManager.orientation = LinearLayout.VERTICAL
        contactRecyclerView.layoutManager = linearLayoutManager
        contactRecyclerView.adapter = contactAdapter

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == SMS_READ_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getSMS()
            } else {
                Toast.makeText(this@HomeActivity, "You must grant the necessary permission.", Toast.LENGTH_SHORT).show()
                showContacts()
            }
        } else if (requestCode == SMS_SEND_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this@HomeActivity, "Get permission, try again.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@HomeActivity, "You must grant the necessary permission.", Toast.LENGTH_SHORT).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
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

            Toast.makeText(ctx, resultString, Toast.LENGTH_LONG).show()
            super.onPostExecute(result)
        }

        override fun onProgressUpdate(vararg values: Void) {
            super.onProgressUpdate(*values)
        }
    }



}

