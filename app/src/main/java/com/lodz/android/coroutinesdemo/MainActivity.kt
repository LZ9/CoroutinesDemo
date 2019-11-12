package com.lodz.android.coroutinesdemo

import android.os.Bundle
import com.google.android.material.button.MaterialButton
import com.lodz.android.corekt.anko.bindView
import com.lodz.android.corekt.anko.getColorCompat
import com.lodz.android.corekt.log.PrintLog
import com.lodz.android.pandora.base.activity.BaseActivity
import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

class MainActivity : BaseActivity() {

    companion object {
        private const val TAG = "testtag"
    }

    private val mRunBlockingDelayBtn by bindView<MaterialButton>(R.id.run_blocking_delay_btn)
    private val mJobJoinBtn by bindView<MaterialButton>(R.id.job_join_btn)
    private val mScopeBtn by bindView<MaterialButton>(R.id.scope_btn)
    private val mRepeatBtn by bindView<MaterialButton>(R.id.repeat_btn)
    private val mCancelBtn by bindView<MaterialButton>(R.id.cancel_btn)
    private val mTimeoutBtn by bindView<MaterialButton>(R.id.timeout_btn)
    private val mSyncBtn by bindView<MaterialButton>(R.id.sync_btn)
    private val mAsyncBtn by bindView<MaterialButton>(R.id.async_btn)


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

        mTimeoutBtn.setOnClickListener {
            timeout()
        }

        mSyncBtn.setOnClickListener {
            sync()
        }

        mAsyncBtn.setOnClickListener {
            async()
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

        /*
         * main : runBlocking 主线程执行
         * DefaultDispatcher-worker-5 : GlobalScope.launch 启动协程，延迟1秒
         * main : runBlocking 主线程延迟2秒执行
         */
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


        /*
         * main : runBlocking 主线程执行
         * DefaultDispatcher-worker-6 : GlobalScope.launch 启动协程，延迟1秒
         */
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

        /*
         * main : 协程作用域 coroutineScope，延迟100毫秒
         * main : launch 启动协程，延迟200毫秒
         * main : 协程作用域 coroutineScope launch 启动，延迟500毫秒
         * main : runBlocking 主线程执行
         */
    }

    /** 启用固定量的协程 */
    private fun repeat(): Job = GlobalScope.launch {
        repeat(10) { i ->
            log("times -> $i")
            delay(100L)
        }
        /*
         * DefaultDispatcher-worker-1 : times -> 0
         * DefaultDispatcher-worker-1 : times -> 1
         * DefaultDispatcher-worker-2 : times -> 2
         * DefaultDispatcher-worker-1 : times -> 3
         * DefaultDispatcher-worker-5 : times -> 4
         * DefaultDispatcher-worker-4 : times -> 5
         * DefaultDispatcher-worker-5 : times -> 6
         * DefaultDispatcher-worker-6 : times -> 7
         * DefaultDispatcher-worker-4 : times -> 8
         * DefaultDispatcher-worker-6 : times -> 9
         */
    }

    /** 取消协程，可以使用isActive判断当前协程是否被取消，也可以使用trycatch来包裹异常对取消后进行处理 */
    private fun cancel(): Job = GlobalScope.launch {
        val startTime = System.currentTimeMillis()
        val job = launch {
//            var nextPrintTime = startTime
//            var i = 0
////            while (isActive) {// 可以使用【isActive】判断协程是否被取消了
//            while (i < 5) { // 一个执行计算的循环，只是为了占用 CPU
//                // 每秒打印消息两次
//                if (System.currentTimeMillis() >= nextPrintTime) {
//                    log("job: I'm sleeping ${i++} ...")
//                    nextPrintTime += 500L
//                }
//            }

            /*
             * DefaultDispatcher-worker-3 : job: I'm sleeping 0 ...
             * DefaultDispatcher-worker-3 : job: I'm sleeping 1 ...
             * DefaultDispatcher-worker-3 : job: I'm sleeping 2 ...
             * DefaultDispatcher-worker-2 : try cancel
             * DefaultDispatcher-worker-3 : job: I'm sleeping 3 ...
             * DefaultDispatcher-worker-3 : job: I'm sleeping 4 ...
             * DefaultDispatcher-worker-3 : already cancel
             */


            try {
                repeat(1000) { i ->
                    log("job I'm sleeping $i ...")
                    delay(500L)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // 运行中的协程被取消会抛出异常JobCancellationException
                log("job exception " + e.toString())
            } finally {
                log("job  I'm running finally")
                withContext(NonCancellable) {
                    // 使用withContext(NonCancellable)可以让协程被取消时依然继续执行逻辑
                    log("job finally 取消后延迟1秒")
                    delay(1000L)
                    log("job finally 完成1秒延迟")
                }
            }

            /*
             * DefaultDispatcher-worker-3 : job I'm sleeping 0 ...
             * DefaultDispatcher-worker-7 : job I'm sleeping 1 ...
             * DefaultDispatcher-worker-4 : job I'm sleeping 2 ...
             * DefaultDispatcher-worker-2 : try cancel
             * DefaultDispatcher-worker-7 : job exception kotlinx.coroutines.JobCancellationException: Job was cancelled; job=StandaloneCoroutine{Cancelling}@e4de0d0
             * DefaultDispatcher-worker-7 : job  I'm running finally
             * DefaultDispatcher-worker-7 : job finally 取消后延迟1秒
             * DefaultDispatcher-worker-6 : job finally 完成1秒延迟
             * DefaultDispatcher-worker-6 : already cancel
             */
        }
        delay(1300L) // 延迟一段时间让job里的逻辑执行
        log("try cancel")
//        job.cancel() // 取消该作业（协程内的逻辑还会执行）
//        job.join() // 等待直到协程内逻辑执行完成
        job.cancelAndJoin() // 取消该作业并等待直到协程内逻辑执行完成
        log("already cancel")
    }

    /** 协程超时，可以使用withTimeoutOrNull包裹，如果超时返回null */
    private fun timeout(): Job = GlobalScope.launch {
//        try {
//            withTimeout(1300L) {
//                repeat(1000) { i ->
//                    log("I'm sleeping $i ...")
//                    delay(500L)
//                }
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//            log("withTimeout exception " + e.toString())
//        }

        /*
         * DefaultDispatcher-worker-1 : I'm sleeping 0 ...
         * DefaultDispatcher-worker-1 : I'm sleeping 1 ...
         * DefaultDispatcher-worker-1 : I'm sleeping 2 ...
         * DefaultDispatcher-worker-4 : withTimeout exception kotlinx.coroutines.TimeoutCancellationException: Timed out waiting for 1300 ms
         */

        // 如果超时则返回null，否则返回你需要的值
        val result = withTimeoutOrNull(3300L) {
            var str = ""
            repeat( 4) { i ->
                str += i
                log("I'm sleeping $i ...")
                delay(500L)
            }
            str // 在它运行得到结果之前取消它
        }
        log("Result is $result")


        /* withTimeoutOrNull(1300L)
         * DefaultDispatcher-worker-1 : I'm sleeping 0 ...
         * DefaultDispatcher-worker-1 : I'm sleeping 1 ...
         * DefaultDispatcher-worker-3 : I'm sleeping 2 ...
         * DefaultDispatcher-worker-4 : Result is null
         */

        /* withTimeoutOrNull(3300L)
         * DefaultDispatcher-worker-1 : I'm sleeping 0 ...
         * DefaultDispatcher-worker-1 : I'm sleeping 1 ...
         * DefaultDispatcher-worker-1 : I'm sleeping 2 ...
         * DefaultDispatcher-worker-4 : I'm sleeping 3 ...
         * DefaultDispatcher-worker-4 : Result is 0123
         */
    }

    /** 协程同步调用（默认同步） */
    private fun sync(): Job = GlobalScope.launch {
        val time = measureTimeMillis {
            val one = doSomethingUsefulOne()
            val two = doSomethingUsefulTwo()
            log("The answer is ${one + two}")
        }
        log("Completed in $time ms")

        /*
         * DefaultDispatcher-worker-1 : The answer is 42
         * DefaultDispatcher-worker-1 : Completed in 1007 ms
         */
    }

    /** 协程异步调用（需要使用async包裹指明异步调用），对象使用await()方法来等待获取执行后的值 */
    private fun async(): Job = GlobalScope.launch {
        val time = measureTimeMillis {
            val one = async { doSomethingUsefulOne() }
            val two = async { doSomethingUsefulTwo() }
            log("The answer is ${one.await() + two.await()}")
        }
        log("Completed in $time ms")

        /*
        * DefaultDispatcher-worker-1 : The answer is 42
        * DefaultDispatcher-worker-1 : Completed in 513 ms
        */
    }

    private suspend fun doSomethingUsefulOne(): Int {
        delay(500L)
        return 13
    }

    private suspend fun doSomethingUsefulTwo(): Int {
        delay(500L)
        return 29
    }


    override fun initData() {
        super.initData()
        showStatusCompleted()
    }

    private fun log(msg: String) {
        PrintLog.dS(TAG, Thread.currentThread().name + " : " + msg)
    }
}
