package com.messagestudio.app.ai

import kotlin.random.Random

/**
 * Built-in sentence pieces for the OFFLINE generator. This is plain template mixing, not AI.
 * {n} is replaced by " Name" when the user chose personalization, or by nothing.
 */
private class Pack(
    val openers: List<String>,
    val closers: List<String>,
    val tone: Map<Tone, List<String>>,
    val bodies: Map<Category, List<String>>
)

private val PACK_EN = Pack(
    openers = listOf("Hello{n},", "Hi{n}!", "Hey{n},", "Good day{n},", "Greetings{n},", "Warm greetings{n},"),
    closers = listOf(
        "Take care.", "Have a wonderful day.", "Best wishes.", "Stay well.",
        "Looking forward to hearing from you.", "Thank you."
    ),
    tone = mapOf(
        Tone.FRIENDLY to listOf("Hope you're doing great.", "Hope life is treating you well."),
        Tone.PROFESSIONAL to listOf("I hope this message finds you well.", "Thank you for your time."),
        Tone.FORMAL to listOf("I trust you are in good health.", "I hope you are keeping well."),
        Tone.CASUAL to listOf("Hope all's good.", "How have you been?"),
        Tone.FUNNY to listOf("Hope your day is going better than my Wi-Fi.", "Hope you've had your tea or coffee by now."),
        Tone.MOTIVATIONAL to listOf("Remember, every day is a fresh start.", "Small steps every day lead to big results.")
    ),
    bodies = mapOf(
        Category.GREETING to listOf(
            "Wishing you a calm and happy day ahead.",
            "Just a quick note to say I'm thinking of you.",
            "May your day be filled with good news and good company.",
            "Sending you good wishes for the day."
        ),
        Category.BIRTHDAY to listOf(
            "Happy birthday! May this year bring you health, success and joy.",
            "Wishing you a very happy birthday and many more to come.",
            "Many happy returns of the day! Enjoy every moment.",
            "On your special day, I wish you all the happiness in the world."
        ),
        Category.BUSINESS to listOf(
            "I wanted to follow up on our recent discussion.",
            "Thank you for the opportunity to work together.",
            "Please let me know a convenient time to talk about the next steps.",
            "I appreciate your continued support and partnership."
        ),
        Category.EDUCATION to listOf(
            "Don't forget to review today's lesson before the next class.",
            "Wishing you the best for your studies and upcoming exams.",
            "Please remember to submit your assignment on time.",
            "Keep learning and keep asking questions; you're doing well."
        ),
        Category.REMINDER to listOf(
            "This is a friendly reminder about our upcoming meeting.",
            "Just a gentle reminder about the task we discussed.",
            "Please remember the appointment scheduled for you.",
            "A polite reminder that the deadline is coming up soon."
        ),
        Category.FRIENDSHIP to listOf(
            "I'm grateful to have a friend like you.",
            "Thanks for always being there for me.",
            "Let's catch up soon; I miss our conversations.",
            "Friends like you make life better."
        ),
        Category.CUSTOM to listOf(
            "I wanted to reach out and say hello.",
            "Thinking of you and wishing you well.",
            "I hope we can talk soon.",
            "Wishing you all the best."
        )
    )
)

private val PACK_RU = Pack(
    openers = listOf("Assalam o Alaikum{n},", "Salam{n},", "Hello{n} ji,", "Kaise hain aap{n}?", "Aadaab{n},", "Hi{n}!"),
    closers = listOf(
        "Apna khayal rakhiye.", "Allah Hafiz.", "Khush rahiye.",
        "Jawab ka intezar rahega.", "Shukriya.", "Dua mein yaad rakhiye."
    ),
    tone = mapOf(
        Tone.FRIENDLY to listOf("Umeed hai aap theek honge.", "Kaisi ja rahi hai zindagi?"),
        Tone.PROFESSIONAL to listOf("Umeed hai aap khairiyat se honge.", "Aap ke qeemti waqt ka shukriya."),
        Tone.FORMAL to listOf("Dua hai ke aap bakhair o aafiyat hon.", "Aap ki khidmat mein salam pesh hai."),
        Tone.CASUAL to listOf("Sab theek thak?", "Kya haal chaal hain?"),
        Tone.FUNNY to listOf("Umeed hai aap ka din mere internet se behtar guzar raha hai.", "Chai pi li ya abhi intezar hai?"),
        Tone.MOTIVATIONAL to listOf("Yaad rakhiye, mehnat kabhi zaya nahi jati.", "Har naya din ek nayi shuruaat hai.")
    ),
    bodies = mapOf(
        Category.GREETING to listOf(
            "Aap ka din khushiyon se bhara guzre.",
            "Bas aap ki yaad aayi, socha salam kar loon.",
            "Allah aap ko hamesha khush rakhe.",
            "Aap ke liye bohat saari dua'en."
        ),
        Category.BIRTHDAY to listOf(
            "Salgirah mubarak! Allah aap ko sehat aur kamyabi de.",
            "Janam din bohat bohat mubarak ho!",
            "Ye khaas din aap ko mubarak, Allah har khushi naseeb kare.",
            "Har saal ki tarah ye saal bhi aap ke liye khushiyan laye."
        ),
        Category.BUSINESS to listOf(
            "Hamari pichli baat cheet ke silsile mein ye paigham hai.",
            "Aap ke saath kaam karna hamare liye bohat acha raha.",
            "Barah-e-meherbani agle qadam ke liye munasib waqt bata dein.",
            "Aap ke taawun ka bohat shukriya."
        ),
        Category.EDUCATION to listOf(
            "Agli class se pehle aaj ka sabaq dohra lein.",
            "Imtihanat ke liye aap ko dheron dua'en.",
            "Barah-e-meherbani apna assignment waqt par jama karwa dein.",
            "Parhai jaari rakhiye, aap bohat acha kar rahe hain."
        ),
        Category.REMINDER to listOf(
            "Ye hamari aane wali meeting ki yaad-dahani hai.",
            "Jo kaam hum ne tay kiya tha uski halki si yaad-dahani.",
            "Barah-e-meherbani apni appointment yaad rakhiye.",
            "Narm yaad-dahani ke aakhri tareekh qareeb hai."
        ),
        Category.FRIENDSHIP to listOf(
            "Mujhe aap jaisa dost milne par fakhar hai.",
            "Hamesha mere saath rehne ka shukriya.",
            "Jald mulaqat karte hain, aap ki bohat yaad aati hai.",
            "Aap jaise dost zindagi ko khoobsurat bana dete hain."
        ),
        Category.CUSTOM to listOf(
            "Socha aap se rabta kar loon.",
            "Aap ko dher saari dua'en.",
            "Umeed hai jald baat hogi.",
            "Aap ke liye bohat achi tamannayen."
        )
    )
)

private val PACK_UR = Pack(
    openers = listOf("السلام علیکم{n}،", "آداب{n}،", "ہیلو{n}،", "خیریت سے ہیں{n}؟", "سلام{n}،", "آپ کو سلام{n}،"),
    closers = listOf(
        "اپنا خیال رکھیے گا۔", "اللہ حافظ۔", "خوش رہیے۔",
        "آپ کے جواب کا انتظار رہے گا۔", "شکریہ۔", "دعاؤں میں یاد رکھیے گا۔"
    ),
    tone = mapOf(
        Tone.FRIENDLY to listOf("امید ہے آپ ٹھیک ہوں گے۔", "زندگی کیسی چل رہی ہے؟"),
        Tone.PROFESSIONAL to listOf("امید ہے آپ خیریت سے ہوں گے۔", "آپ کے قیمتی وقت کا شکریہ۔"),
        Tone.FORMAL to listOf("دعا ہے کہ آپ بخیر و عافیت ہوں۔", "آپ کی خدمت میں سلام پیش ہے۔"),
        Tone.CASUAL to listOf("سب ٹھیک ٹھاک؟", "کیا حال چال ہیں؟"),
        Tone.FUNNY to listOf("امید ہے آپ کا دن میرے انٹرنیٹ سے بہتر گزر رہا ہے۔", "چائے پی لی یا ابھی انتظار ہے؟"),
        Tone.MOTIVATIONAL to listOf("یاد رکھیے، محنت کبھی ضائع نہیں جاتی۔", "ہر نیا دن ایک نئی شروعات ہے۔")
    ),
    bodies = mapOf(
        Category.GREETING to listOf(
            "آپ کا دن خوشیوں بھرا گزرے۔",
            "بس آپ کی یاد آئی، سوچا سلام کر لوں۔",
            "اللہ آپ کو ہمیشہ خوش رکھے۔",
            "آپ کے لیے بہت ساری دعائیں۔"
        ),
        Category.BIRTHDAY to listOf(
            "سالگرہ مبارک! اللہ آپ کو صحت اور کامیابی دے۔",
            "جنم دن بہت بہت مبارک ہو!",
            "یہ خاص دن آپ کو مبارک، اللہ ہر خوشی نصیب کرے۔",
            "ہر سال کی طرح یہ سال بھی آپ کے لیے خوشیاں لائے۔"
        ),
        Category.BUSINESS to listOf(
            "ہماری پچھلی گفتگو کے سلسلے میں یہ پیغام ہے۔",
            "آپ کے ساتھ کام کر کے ہمیں بہت اچھا لگا۔",
            "براہِ مہربانی اگلے قدم کے لیے مناسب وقت بتا دیں۔",
            "آپ کے تعاون کا بہت شکریہ۔"
        ),
        Category.EDUCATION to listOf(
            "اگلی کلاس سے پہلے آج کا سبق دہرا لیں۔",
            "امتحانات کے لیے آپ کو ڈھیروں دعائیں۔",
            "براہِ مہربانی اپنا اسائنمنٹ وقت پر جمع کروا دیں۔",
            "پڑھائی جاری رکھیے، آپ بہت اچھا کر رہے ہیں۔"
        ),
        Category.REMINDER to listOf(
            "یہ ہماری آنے والی میٹنگ کی یاد دہانی ہے۔",
            "جو کام ہم نے طے کیا تھا اس کی ہلکی سی یاد دہانی۔",
            "براہِ مہربانی اپنی اپائنٹمنٹ یاد رکھیے۔",
            "نرم یاد دہانی کہ آخری تاریخ قریب ہے۔"
        ),
        Category.FRIENDSHIP to listOf(
            "مجھے آپ جیسا دوست ملنے پر فخر ہے۔",
            "ہمیشہ میرے ساتھ رہنے کا شکریہ۔",
            "جلد ملاقات کرتے ہیں، آپ کی بہت یاد آتی ہے۔",
            "آپ جیسے دوست زندگی کو خوبصورت بنا دیتے ہیں۔"
        ),
        Category.CUSTOM to listOf(
            "سوچا آپ سے رابطہ کر لوں۔",
            "آپ کو ڈھیر ساری دعائیں۔",
            "امید ہے جلد بات ہوگی۔",
            "آپ کے لیے بہت اچھی تمنائیں۔"
        )
    )
)

private val EMOJI = mapOf(
    Category.GREETING to listOf("😊", "🌸", "☀️"),
    Category.BIRTHDAY to listOf("🎂", "🎉", "🎈"),
    Category.BUSINESS to listOf("🤝", "📈", "✅"),
    Category.EDUCATION to listOf("📚", "✏️", "🎓"),
    Category.REMINDER to listOf("⏰", "📌", "🔔"),
    Category.FRIENDSHIP to listOf("🤗", "💛", "😊"),
    Category.CUSTOM to listOf("🙂", "✨", "👍")
)

private fun packFor(language: Language, rnd: Random): Pack = when (language) {
    Language.ENGLISH -> PACK_EN
    Language.ROMAN_URDU -> PACK_RU
    Language.URDU -> PACK_UR
    Language.MIXED -> if (rnd.nextBoolean()) PACK_EN else PACK_RU
}

/** Builds ONE message by mixing pieces. The same call can return a message made before. */
fun composeLocalMessage(config: GenerationConfig, rnd: Random): String {
    val a = packFor(config.language, rnd)
    val b = packFor(config.language, rnd)
    val name = config.recipientName?.trim().orEmpty()
    val n = if (name.isEmpty()) "" else " $name"

    val opener = a.openers.random(rnd).replace("{n}", n)
    val bodies = b.bodies.getValue(config.category)
    val body = bodies.random(rnd)
    val tonePhrase = a.tone.getValue(config.tone).random(rnd)
    val closer = a.closers.random(rnd)

    val parts = when (config.length) {
        LengthOption.SHORT -> listOf(opener, body)
        LengthOption.MEDIUM -> listOf(opener, tonePhrase, body, closer)
        LengthOption.LONG -> listOf(opener, tonePhrase, body, bodies.filter { it != body }.random(rnd), closer)
    }
    val text = parts.joinToString(" ")

    val pool = EMOJI.getValue(config.category)
    return when (config.emoji) {
        EmojiMode.OFF -> text
        EmojiMode.MINIMAL -> "$text ${pool.random(rnd)}"
        EmojiMode.ENABLED -> "${pool.random(rnd)} $text ${pool.random(rnd)}"
    }
}
