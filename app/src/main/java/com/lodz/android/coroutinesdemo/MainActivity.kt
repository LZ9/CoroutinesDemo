package com.lodz.android.coroutinesdemo

import android.os.Bundle
import com.google.android.material.button.MaterialButton
import com.lodz.android.corekt.anko.bindView
import com.lodz.android.corekt.anko.getColorCompat
import com.lodz.android.corekt.log.PrintLog
import com.lodz.android.pandora.base.activity.BaseActivity
import kotlinx.coroutines.*

class MainActivity : BaseActivity() {

    companion object{
        private const val TAG = "testtag"
    }

    private val mRunBlockingDelayBtn by bindView<MaterialButton>(R.id.run_blocking_delay_btn)
    private val mJobJoinBtn by bindView<MaterialButton>(R.id.job_join_btn)
    private val mScopeBtn by bindView<MaterialButton>(R.id.scope_btn)
    private val mRepeatBtn by bindView<MaterialButton>(R.id.repeat_btn)
    private val mCancelBtn by bindView<MaterialButton>(R.id.cancel_btn)

    override fun getLayoutId(): Int = R.layout.activity_main

    override fun findViews(savedInstanceState: Bundle?) {
        super.findViews(savedInstanceState)
        initTitleBarLayout()
    }

    private fun initTitleBarLayout() {
        getTitleBarLayout().setTitleName(R.string.app_name)
        getTitleBarLayout().setBackgroundColor(getColorCompat(R.color.colorPrimary))
        getTitleBarLayout().needBackButton(false)
    }


    override fun setListeners() {
        super.setListeners()
        mRunBlockingDelayBtn.setOnClickListener {
            runBlockingDelay()
        }

        mJobJoinBtn.setOnClickListener {
            jobJoin()
        }

        mScopeBtn.setOnClickListener {
            scope()
        }

        mRepeatBtn.setOnClickListener {
            repeat()
        }

        mCancelBtn.setOnClickListener {
            cancel()
        }
    }

    /** runBlocking执行在主线程，GlobalScope.launch开启一个协程 */
    private fun runBlockingDelay(): Unit = runBlocking {
        // 在后台启动一个新的协程并继续
        GlobalScope.launch {
            delay(1000)
            log("GlobalScope.launch 启动协程，延迟1秒")
        }
        log("runBlocking 主线程执行")// 主协程在这里会立即执行
        delay(2000)
        log("runBlocking 主线程延迟2秒执行")
    }

    /** 使用job的join方法来阻塞当前线程等待子协程执行完毕 */
    private fun jobJoin() = runBlocking {
        // 启动一个新协程并保持对这个作业的引用
        val job = GlobalScope.launch {
            delay(1000)
            log("GlobalScope.launch 启动协程，延迟1秒")
        }
        log("runBlocking 主线程执行")
        job.join()// 阻塞主线程等待直到子协程执行结束
    }

    /** coroutineScope在所有已启动子协程执行完毕之前不会结束 */
    private fun scope() = runBlocking {
        launch {
            delay(200L)
            log("launch 启动协程，延迟200毫秒")
        }
        // 创建一个协程作用域
        coroutineScope {
            launch {
                delay(500L)
                log("协程作用域 coroutineScope launch 启动，延迟500毫秒")
            }
            delay(100L)
            log("协程作用域 coroutineScope，延迟100毫秒")
        }
        log("runBlocking 主线程执行")
    }

    /** 启用固定量的协程 */
    private fun repeat(): Job = GlobalScope.launch {
        repeat(10) { i ->
            log("times -> $i")
            delay(100L)
        }
    }

    /** 取消协程 */
    private fun cancel(): Job = GlobalScope.launch {
        val startTime = System.currentTimeMillis()
        val job = launch {
            var nextPrintTime = startTime
            var i = 0
//            while (isActive) {// 可以使用【isActive】判断协程是否被取消了
            while (i < 5) { // 一个执行计算的循环，只是为了占用 CPU
                // 每秒打印消息两次
                if (System.currentTimeMillis() >= nextPrintTime) {
                    log("job: I'm sleeping ${i++} ...")
                    nextPrintTime += 500L
                }
            }
        }
        delay(1300L) // 延迟一段时间让job里的逻辑执行
        log("try cancel")
//        job.cancel() // 取消该作业（协程内的逻辑还会执行）
//        job.join() // 等待直到协程内逻辑执行完成
        job.cancelAndJoin() // 取消该作业并等待直到协程内逻辑执行完成
        log("already cancel")
    }

    override fun initData() {
        super.initData()
        showStatusCompleted()
    }

    private fun log(msg: String) {
        PrintLog.dS(TAG, Thread.currentThread().name + " : " + msg)
    }
}
