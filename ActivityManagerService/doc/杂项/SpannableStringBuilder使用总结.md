# SpannableStringBuilder使用总结 #

## 目录 ##

1. SpannableStringBuilder概述
2. SpannableStringBuilder示例
3. SpannableStringBuilder注意事项

## SpannableStringBuilder概述 ##

SpannableStringBuilder 用于组合生成特殊风格的 CharSequence，可以显示多种样式的TextView。  
可以设置每个字的样式，颜色，大小等。甚至可以插入图片。

[详情参考大牛博客](http://blog.csdn.net/qq_16430735/article/details/50427978)

SpannableStringBuilder 与StringBuilder类似，可以动态增删字符，而且可以添加多个Span属性到同一段字符中。


## SpannableStringBuilder示例 ##

将前一段字符的颜色改变：

```
public static SpannableStringBuilder retText(String left, String right){
        SpannableStringBuilder spannableString = new SpannableStringBuilder(left+right);
        ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.parseColor("#CCFFFFFF"));
        spannableString.setSpan(colorSpan, 0, left.length(), Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
        return spannableString;
    }
```

setSpan(Object what, int start, int end, int flags)

what:设置什么Span
start:起始位置
end:结束位置
flags:标记位  常见的有四个：
Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
Spannable.SPAN_EXCLUSIVE_INCLUSIVE
Spannable.SPAN_INCLUSIVE_EXCLUSIVE
Spannable.SPAN_INCLUSIVE_INCLUSIVE
前一个EXCLUSIVE(INCLUSIVE)指明 start位置(是)否被算在新样式中
前一个EXCLUSIVE(INCLUSIVE)指明 end位置(是)否被算在新样式中

## SpannableStringBuilder示例 ##

1. 不要出现`spannableString.setSpan(colorSpan, i, i, Spannable.SPAN_INCLUSIVE_INCLUSIVE);`的写法，当end-start=0时会忽略Span改动。若有需要按字符改动，建议可以写`spannableString.setSpan(colorSpan, i, i+1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);`进行逐位修改Span。



2. 每类Span若设置多次，只有最后一次生效。若希望多次设置同一类Span都生效，可以设置`CharacterStyle.warp`。如：

```
for (int i=0;i<str.length();i++){
                spannableString.setSpan(CharacterStyle.wrap(colorSpan), i, i+1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        }
```

通过以上方式，可以逐位进行设置Span，也可以设置多个Span.