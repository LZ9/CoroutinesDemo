package com.lodz.android.coroutinesdemo

import android.os.Bundle
import com.google.android.material.button.MaterialButton
import com.lodz.android.corekt.anko.bindView
import com.lodz.android.corekt.anko.getColorCompat
import com.lodz.android.corekt.log.PrintLog
import com.lodz.android.pandora.base.activity.BaseActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
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
    private val mLazyAsyncBtn by bindView<MaterialButton>(R.id.lazy_async_btn)
    private val mDispatcherBtn by bindView<MaterialButton>(R.id.dispatcher_btn)
    private val mFlowBtn by bindView<MaterialButton>(R.id.flow_btn)
    private val mTimeoutFlowBtn by bindView<MaterialButton>(R.id.timeout_flow_btn)
    private val mTransformTakeFlowBtn by bindView<MaterialButton>(R.id.transform_take_flow_btn)
    private val mZipFlowBtn by bindView<MaterialButton>(R.id.zip_flow_btn)
    private val mChannelBtn by bindView<MaterialButton>(R.id.channel_btn)
    private val mCloseChannelBtn by bindView<MaterialButton>(R.id.close_channel_btn)



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


    @FlowPreview
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

        mLazyAsyncBtn.setOnClickListener {
            lazyAsync()
        }

        mDispatcherBtn.setOnClickListener {
           dispatcher()
        }

        mFlowBtn.setOnClickListener {
            flowTest()
        }

        mTimeoutFlowBtn.setOnClickListener {
            timeoutFlow()
        }

        mTransformTakeFlowBtn.setOnClickListener {
            transformTakeFlow()
        }

        mZipFlowBtn.setOnClickListener {
            zipFlow()
        }

        mChannelBtn.setOnClickListener {
            channel()
        }

        mCloseChannelBtn.setOnClickListener {
            closeChannel()
        }
    }

    /** runBlocking执行在主线程，GlobalScope.launch开启一个协程 */
    private fun runBlockingDelay(): Unit = runBlocking {
        // 在后台启动一个新的协程并继续
        GlobalScope.launch {
            delay(1000)
            logd("GlobalScope.launch 启动协程，延迟1秒")
        }
        logd("runBlocking 主线程执行")// 主协程在这里会立即执行
        delay(2000)
        logd("runBlocking 主线程延迟2秒执行")

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
            logd("GlobalScope.launch 启动协程，延迟1秒")
        }
        logd("runBlocking 主线程执行")
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
            logd("launch 启动协程，延迟200毫秒")
        }
        // 创建一个协程作用域
        coroutineScope {
            launch {
                delay(500L)
                logd("协程作用域 coroutineScope launch 启动，延迟500毫秒")
            }
            delay(100L)
            logd("协程作用域 coroutineScope，延迟100毫秒")
        }
        logd("runBlocking 主线程执行")

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
            logd("times -> $i")
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
//                    logd("job: I'm sleeping ${i++} ...")
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
                    logd("job I'm sleeping $i ...")
                    delay(500L)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // 运行中的协程被取消会抛出异常JobCancellationException
                logd("job exception " + e.toString())
            } finally {
                logd("job  I'm running finally")
                withContext(NonCancellable) {
                    // 使用withContext(NonCancellable)可以让协程被取消时依然继续执行逻辑
                    logd("job finally 取消后延迟1秒")
                    delay(1000L)
                    logd("job finally 完成1秒延迟")
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
        logd("try cancel")
//        job.cancel() // 取消该作业（协程内的逻辑还会执行）
//        job.join() // 等待直到协程内逻辑执行完成
        job.cancelAndJoin() // 取消该作业并等待直到协程内逻辑执行完成
        logd("already cancel")
    }

    /** 协程超时，可以使用withTimeoutOrNull包裹，如果超时返回null */
    private fun timeout(): Job = GlobalScope.launch {
//        try {
//            withTimeout(1300L) {
//                repeat(1000) { i ->
//                    logd("I'm sleeping $i ...")
//                    delay(500L)
//                }
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//            logd("withTimeout exception " + e.toString())
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
                logd("I'm sleeping $i ...")
                delay(500L)
            }
            str // 在它运行得到结果之前取消它
        }
        logd("Result is $result")


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
            logd("The answer is ${one + two}")
        }
        logd("Completed in $time ms")

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
            logd("The answer is ${one.await() + two.await()}")
        }
        logd("Completed in $time ms")

        /*
        * DefaultDispatcher-worker-1 : The answer is 42
        * DefaultDispatcher-worker-1 : Completed in 513 ms
        */
    }

    /** 协程懒加载异步调用（需要指明start = CoroutineStart.LAZY），对象使用start()或者await()方法后才会执行协程里的逻辑 */
    private fun lazyAsync(): Job = GlobalScope.launch {
        val time = measureTimeMillis {
//            val one = async(start = CoroutineStart.LAZY) { doSomethingUsefulOne() }
//            val two = async(start = CoroutineStart.LAZY) { doSomethingUsefulTwo() }
//            delay(1000)
//            one.start() // 启动第一个
//            two.start() // 启动第二个
//            logd("The answer is ${one.await() + two.await()}")

            /*
             * DefaultDispatcher-worker-1 : The answer is 42
             * DefaultDispatcher-worker-1 : Completed in 1514 ms
             */


            logd("The answer is ${concurrentSum()}")
            /*
             * DefaultDispatcher-worker-3 : The answer is 42
             * DefaultDispatcher-worker-3 : Completed in 512 ms
             */
        }
        logd("Completed in $time ms")

        /*
         * DefaultDispatcher-worker-1 : The answer is 42
         * DefaultDispatcher-worker-1 : Completed in 1514 ms
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

    /** 将两个异步协程方法组合成一个协程域 */
    private suspend fun concurrentSum(): Int = coroutineScope {
        val one = async { doSomethingUsefulOne() }
        val two = async { doSomethingUsefulTwo() }
        one.await() + two.await()
    }

    /** 协程调度器 */
    private fun dispatcher()  = runBlocking {
        launch {
            logd("launch")
        }
        GlobalScope.launch {
            logd("GlobalScope.launch")
        }
        GlobalScope.launch(Dispatchers.Main) {
            logd("GlobalScope.launch(Dispatchers.Main)")
        }
        GlobalScope.launch(Dispatchers.IO) {
            logd("GlobalScope.launch(Dispatchers.IO)")
        }
        GlobalScope.launch(Dispatchers.Default) {
            logd("GlobalScope.launch(Dispatchers.Default)")
        }

        /*
         * DefaultDispatcher-worker-1 : GlobalScope.launch
         * DefaultDispatcher-worker-1 : GlobalScope.launch(Dispatchers.IO)
         * DefaultDispatcher-worker-3 : GlobalScope.launch(Dispatchers.Default)
         * main : launch
         * main : GlobalScope.launch(Dispatchers.Main)
         */
    }


    /** 流构建器，每当collect的时候才会执行flow里的逻辑 */
    @FlowPreview
    private fun flows(): Flow<Int> = flow {
        // 流构建器
        for (i in 1..3) {
            delay(100) // 假装我们在这里做了一些有用的事情
            logi("flowTest emit : $i")
            emit(i) // 发送下一个值
        }
    }

    /** 流测试，多次collect */
    @FlowPreview
    private fun flowTest(): Job = GlobalScope.launch {
        // 多次收集流
        loge("collect first")
        flows().collect { value -> logd("flowTest collect : $value") }
        loge("collect again")
        flows().collect { value -> logd("flowTest collect : $value") }

        /*
         * DefaultDispatcher-worker-1 : collect first
         * DefaultDispatcher-worker-1 : flowTest emit : 1
         * DefaultDispatcher-worker-1 : flowTest collect : 1
         * DefaultDispatcher-worker-1 : flowTest emit : 2
         * DefaultDispatcher-worker-1 : flowTest collect : 2
         * DefaultDispatcher-worker-3 : flowTest emit : 3
         * DefaultDispatcher-worker-3 : flowTest collect : 3
         * DefaultDispatcher-worker-3 : collect again
         * DefaultDispatcher-worker-3 : flowTest emit : 1
         * DefaultDispatcher-worker-3 : flowTest collect : 1
         * DefaultDispatcher-worker-3 : flowTest emit : 2
         * DefaultDispatcher-worker-3 : flowTest collect : 2
         * DefaultDispatcher-worker-1 : flowTest emit : 3
         * DefaultDispatcher-worker-1 : flowTest collect : 3
         */
    }

    /** 控制流超时返回null，map操作符可以对流数据进行过渡操作 */
    @FlowPreview
    private fun timeoutFlow(): Job = GlobalScope.launch {
        // 在 250 毫秒后超时
        val result = withTimeoutOrNull(250) {
            // 将一个整数区间转化为流
            (1..5).asFlow()
                .map { value ->
                    delay(100)
                    value
                }
                .collect { value -> logd("timeoutFlow collect : $value") }
            "timeoutFlow success"
        }
        loge("timeoutFlow result : $result")

        /*
         * DefaultDispatcher-worker-1 : timeoutFlow collect : 1
         * DefaultDispatcher-worker-3 : timeoutFlow collect : 2
         * DefaultDispatcher-worker-3 : timeoutFlow result : null
         */
    }

    /** transform可以进行数据控制决定要把哪些数据给下游，take可以指定获取前n个数据 */
    @FlowPreview
    private fun transformTakeFlow(): Job = GlobalScope.launch {
        (1..5).asFlow()
            .take(3)
            .transform { value ->
                logi("timeoutFlow take : $value")
                if (value % 2 == 1) {
                    emit(value)
                }
            }
            .collect { value -> logd("timeoutFlow collect : $value") }

        /*
         * DefaultDispatcher-worker-2 : timeoutFlow take : 1
         * DefaultDispatcher-worker-2 : timeoutFlow collect : 1
         * DefaultDispatcher-worker-2 : timeoutFlow take : 2
         * DefaultDispatcher-worker-2 : timeoutFlow take : 3
         * DefaultDispatcher-worker-2 : timeoutFlow collect : 3
         */
    }

    /** zip合并两个flow组合的数据 */
    @FlowPreview
    private fun zipFlow(): Job = GlobalScope.launch {
        val nums = (1..3).asFlow() // 数字 1..3
        val strs = flowOf("one", "two", "three") // 字符串
        nums.zip(strs) { a, b -> "$a -> $b" } // 组合单个字符串
            .collect { value -> logd("timeoutFlow collect : $value") } // 收集并打印

        /*
         * DefaultDispatcher-worker-2 : timeoutFlow collect : 1 -> one
         * DefaultDispatcher-worker-2 : timeoutFlow collect : 2 -> two
         * DefaultDispatcher-worker-5 : timeoutFlow collect : 3 -> three
         */
    }

    /** channel通道每send一次receive就回调一次 */
    private fun channel(): Job = GlobalScope.launch {
        val channel = Channel<Int>()
        launch {
            // 这里可能是消耗大量 CPU 运算的异步逻辑，我们将仅仅做 5 次整数的平方并发送
            for (x in 1..5) {
                delay(200)
                logi("channel x : $x")
                channel.send(x * x)
            }
        }
        // 这里我们打印了 5 次被接收的整数：
        repeat(5) {
            logd(channel.receive().toString())
        }
        loge("Done!")

        /*
         * DefaultDispatcher-worker-1 : channel x : 1
         * DefaultDispatcher-worker-1 : 1
         * DefaultDispatcher-worker-4 : channel x : 2
         * DefaultDispatcher-worker-2 : 4
         * DefaultDispatcher-worker-4 : channel x : 3
         * DefaultDispatcher-worker-7 : 9
         * DefaultDispatcher-worker-8 : channel x : 4
         * DefaultDispatcher-worker-7 : 16
         * DefaultDispatcher-worker-7 : channel x : 5
         * DefaultDispatcher-worker-1 : 25
         * DefaultDispatcher-worker-1 : Done!
         */
    }

    private fun closeChannel(): Job = GlobalScope.launch {
        val channel = Channel<Int>()
        launch {
            for (x in 1..5) {
                channel.send(x * x)
                if (x == 4){
                    channel.close() // 我们结束发送
                }
            }
        }
        // 这里我们使用 `for` 循环来打印所有被接收到的元素（直到通道被关闭）
        for (y in channel) {
            logd(y.toString())
        }
        loge("Done!")

        /*
         * DefaultDispatcher-worker-5 : 1
         * DefaultDispatcher-worker-5 : 4
         * DefaultDispatcher-worker-5 : 9
         * DefaultDispatcher-worker-5 : 16
         * DefaultDispatcher-worker-1 : Done!
         */
    }

    override fun initData() {
        super.initData()
        showStatusCompleted()
    }

    private fun logd(msg: String) {
        PrintLog.dS(TAG, Thread.currentThread().name + " : " + msg)
    }

    private fun loge(msg: String) {
        PrintLog.eS(TAG, Thread.currentThread().name + " : " + msg)
    }

    private fun logi(msg: String) {
        PrintLog.iS(TAG, Thread.currentThread().name + " : " + msg)
    }
}
