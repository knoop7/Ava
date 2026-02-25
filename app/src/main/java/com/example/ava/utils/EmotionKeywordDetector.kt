package com.example.ava.utils

object EmotionKeywordDetector {
    
    enum class Expression {
        NEUTRAL,
        HAPPY,
        SAD,
        SURPRISED,
        THINKING,
        SLEEPY,
        EXCITED,
        CONFUSED,
        LISTENING,
        SPEAKING,
        ANGRY,
        SHY,
        PROUD,
        CURIOUS
    }

    private val HAPPY_KEYWORDS = listOf(
        "开心", "高兴", "快乐", "太好了", "棒", "厉害", "恭喜", "祝贺", "成功", "完成",
        "好的", "没问题", "当然", "可以", "好啊", "哈哈", "嘻嘻", "耶", "赞", "喜欢",
        "很好", "不错", "优秀", "满意", "愉快", "欢迎", "感谢", "谢谢",
        "happy", "great", "good", "nice", "wonderful", "excellent", "amazing", "awesome",
        "congratulations", "perfect", "love", "like", "yes", "sure", "okay", "welcome", "thanks"
    )
    
    private val SAD_KEYWORDS = listOf(
        "难过", "伤心", "遗憾", "可惜", "抱歉", "对不起", "不好意思", "失败", "错误",
        "无法", "不能", "做不到", "很遗憾", "不幸", "糟糕", "唉", "哎", "可怜",
        "sorry", "sad", "unfortunately", "failed", "cannot", "unable", "regret", "apologize",
        "mistake", "error", "wrong"
    )
    
    private val SURPRISED_KEYWORDS = listOf(
        "哇", "天哪", "真的吗", "不会吧", "居然", "竟然", "没想到", "意外", "惊讶",
        "震惊", "不敢相信", "太神奇", "不可思议", "哦", "啊",
        "wow", "really", "amazing", "incredible", "unbelievable", "surprising", "shocked", "oh"
    )
    
    private val THINKING_KEYWORDS = listOf(
        "让我想想", "思考", "考虑", "分析", "研究", "查一下", "看看", "嗯",
        "这个问题", "关于这个", "我认为", "我觉得", "可能是", "也许", "或许",
        "let me think", "thinking", "consider", "analyze", "perhaps", "maybe", "probably",
        "hmm", "well"
    )
    
    private val SLEEPY_KEYWORDS = listOf(
        "晚安", "睡觉", "休息", "累了", "困了", "疲惫", "放松", "再见", "拜拜",
        "goodnight", "sleep", "tired", "rest", "relax", "goodbye", "bye"
    )
    
    private val EXCITED_KEYWORDS = listOf(
        "太棒了", "超级", "非常", "极其", "特别", "超", "爆", "绝了", "牛",
        "激动", "兴奋", "期待", "迫不及待", "太厉害", "太强了", "666",
        "super", "extremely", "absolutely", "fantastic", "incredible", "excited", "thrilled",
        "awesome", "brilliant"
    )
    
    private val CONFUSED_KEYWORDS = listOf(
        "不明白", "不理解", "什么意思", "怎么回事", "为什么", "奇怪", "困惑",
        "搞不懂", "不清楚", "迷惑", "疑惑", "纳闷",
        "confused", "don't understand", "what", "why", "strange", "weird", "unclear"
    )
    
    private val LISTENING_KEYWORDS = listOf(
        "请说", "我在听", "继续", "然后呢", "接着说", "告诉我", "请讲",
        "listening", "go on", "continue", "tell me", "please"
    )
    

    private val SPEAKING_KEYWORDS = listOf(
        "我来", "让我", "首先", "接下来", "总结", "解释", "说明", "介绍",
        "let me", "first", "next", "finally", "in summary", "to explain", "here"
    )
    
    private val ANGRY_KEYWORDS = listOf(
        "生气", "愤怒", "讨厌", "烦", "气死", "可恶", "混蛋", "该死", "滚",
        "不行", "绝对不", "休想", "别想", "不许", "警告",
        "angry", "mad", "hate", "annoying", "damn", "hell", "no way", "never", "warning"
    )
    
    private val SHY_KEYWORDS = listOf(
        "害羞", "不好意思", "羞", "脸红", "嘿嘿", "呵呵", "嗯嗯", "那个",
        "shy", "blush", "embarrassed", "hehe", "umm"
    )
    
    private val PROUD_KEYWORDS = listOf(
        "当然", "那是", "必须的", "小意思", "简单", "轻松", "没问题啦", "包在我身上",
        "我最擅长", "我很厉害", "交给我",
        "of course", "easy", "no problem", "leave it to me", "i'm good at", "piece of cake"
    )
    
    private val CURIOUS_KEYWORDS = listOf(
        "有趣", "好奇", "想知道", "是什么", "怎么样", "如何", "告诉我更多",
        "interesting", "curious", "wonder", "what is", "how", "tell me more", "fascinating"
    )
    
    fun detectExpression(text: String?): Expression {
        if (text.isNullOrBlank()) return Expression.NEUTRAL
        
        val lowerText = text.lowercase()
        
        if (ANGRY_KEYWORDS.any { lowerText.contains(it.lowercase()) }) return Expression.ANGRY
        if (EXCITED_KEYWORDS.any { lowerText.contains(it.lowercase()) }) return Expression.EXCITED
        if (SURPRISED_KEYWORDS.any { lowerText.contains(it.lowercase()) }) return Expression.SURPRISED
        if (HAPPY_KEYWORDS.any { lowerText.contains(it.lowercase()) }) return Expression.HAPPY
        if (PROUD_KEYWORDS.any { lowerText.contains(it.lowercase()) }) return Expression.PROUD
        if (SAD_KEYWORDS.any { lowerText.contains(it.lowercase()) }) return Expression.SAD
        if (SHY_KEYWORDS.any { lowerText.contains(it.lowercase()) }) return Expression.SHY
        if (CURIOUS_KEYWORDS.any { lowerText.contains(it.lowercase()) }) return Expression.CURIOUS
        if (CONFUSED_KEYWORDS.any { lowerText.contains(it.lowercase()) }) return Expression.CONFUSED
        if (THINKING_KEYWORDS.any { lowerText.contains(it.lowercase()) }) return Expression.THINKING
        if (SLEEPY_KEYWORDS.any { lowerText.contains(it.lowercase()) }) return Expression.SLEEPY
        if (LISTENING_KEYWORDS.any { lowerText.contains(it.lowercase()) }) return Expression.LISTENING
        if (SPEAKING_KEYWORDS.any { lowerText.contains(it.lowercase()) }) return Expression.SPEAKING
        
        return Expression.NEUTRAL
    }
}
