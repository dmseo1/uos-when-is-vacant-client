package com.dongmin.www.wiv.libraries

import com.dongmin.www.wiv.activities.Init.StaticData.basicURL
import android.os.AsyncTask
import android.os.Handler
import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.*


//분산서버용 커넥터
class HttpConnectorOthers constructor(private val path : String, private val param : String, private val listener : UIModifyAvailableListener?) : AsyncTask<Void, Int, String>() {

    private var timeOutThread = Thread() //타임아웃 스레드
    private var result = ""
    private var handler : Handler? = null

    override fun onPreExecute() {
        try {
            handler = Handler()
            //타임아웃 스레드 설정
            timeOutThread = Thread {
                try {
                    Thread.sleep(10000)
                    handler!!.post {
                        onPostExecute("NETWORK_CONNECTION_UNSTABLE")
                    }
                    this.cancel(true)
                } catch(e : InterruptedException) {

                }
            }
            timeOutThread.start()
        } catch(e : Exception) {
            return
        }
    }

    override fun doInBackground(vararg params: Void?): String {


        //TODO: 호출 파일명과 파라미터 확인(출시 시 제거)
       // Log.d(path, param)

        try {
            val url = URL(this.path)
            val conn = url.openConnection() as HttpURLConnection

            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.requestMethod = "POST"
            conn.doInput = true
            try {
                conn.connect()
               // Log.d("리스폰스1", "${conn.responseCode}: ${conn.responseMessage}")
            } catch(e : ConnectException) {
                //Log.e("여기", "야")
                return "NETWORK_CONNECTION_FAILED"
            } catch(e : IOException) {
                //Log.e("여기", "니")
                return "NETWORK_CONNECTION_FAILED"
            } catch(e : UnknownHostException) {
                //Log.e("여기","와서 걸리는거 아님?")
                //e.printStackTrace()
                return "NETWORK_CONNECTION_FAILED"
            } catch(e : Exception) {
                e.printStackTrace()
                return "NETWORK_CONNECTION_FAILED"
            }

            //Log.d("리스폰스2", "${conn.responseCode}: ${conn.responseMessage}")

            //안드로이드 -> 서버
            val outs = conn.outputStream
            outs.write(this.param.toByteArray())
            //Log.d("REAL_PARAM", this.param.toByteArray(charset("UTF-8")).toString())
            outs.flush()
            outs.close()

            //서버 -> 안드로이드
            val data : String
            val inputStream: InputStream?
            val bufferedReader: BufferedReader?

            inputStream = conn.inputStream
            bufferedReader = BufferedReader(InputStreamReader(inputStream!!), 8 * 1024)
            var line : String?
            val buff = StringBuffer()
            // if(bufferedReader.readLine() == null) Log.d("wow", "wow")
            while (true) {
                line =  bufferedReader.readLine()
                if(line == null) break
                else buff.append(line + "\n")
            }

            data = buff.toString().trim { it <= ' ' }
            //Log.e("RECV DATA", data)
            result = data

        } catch (e: MalformedURLException) {
            //Log.e("여기", "군")
            //e.printStackTrace()
            result = "NETWORK_CONNECTION_FAILED"
        } catch (e: IOException) {
            //Log.e("여기", "요")
            //e.printStackTrace()
            result = "NETWORK_CONNECTION_FAILED"
        } catch(e: Exception) {
            //e.printStackTrace()
            result = "NETWORK_CONNECTION_FAILED"
        }
        return result
    }

    override fun onPostExecute(result: String) {
        super.onPostExecute(result)
        if(timeOutThread.isAlive) {
            timeOutThread.interrupt()
        }
        this.listener!!.taskCompleted(result)
    }
}