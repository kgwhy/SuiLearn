Add-Type -AssemblyName System.Drawing

$out = "D:\SuiLearn\docs\ui-mockups\suilearn-home-v3.png"
$bmp = [System.Drawing.Bitmap]::new(390, 844)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::ClearTypeGridFit

function Brush($hex) { [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml($hex)) }
function PenC($hex, $w = 1) { [System.Drawing.Pen]::new([System.Drawing.ColorTranslator]::FromHtml($hex), $w) }
function FontC($em, $style = [System.Drawing.FontStyle]::Regular) { [System.Drawing.Font]::new("Microsoft YaHei UI", [single]$em, $style, [System.Drawing.GraphicsUnit]::Pixel) }
function RR($x, $y, $w, $h, $r) {
    $p = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $d = $r * 2
    $p.AddArc($x, $y, $d, $d, 180, 90)
    $p.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
    $p.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0, 90)
    $p.AddArc($x, $y + $h - $d, $d, $d, 90, 90)
    $p.CloseFigure()
    $p
}
function FillRR($x, $y, $w, $h, $r, $c) { $g.FillPath((Brush $c), (RR $x $y $w $h $r)) }
function StrokeRR($x, $y, $w, $h, $r, $c, $sw = 1) { $g.DrawPath((PenC $c $sw), (RR $x $y $w $h $r)) }
function Txt($s, $x, $y, $fontSize, $c, $style = [System.Drawing.FontStyle]::Regular) {
    $font = FontC -em $fontSize -style $style
    $g.DrawString($s, $font, (Brush $c), [single]$x, [single]$y)
}
function L($x1, $y1, $x2, $y2, $c, $sw = 2) {
    $pen = PenC $c $sw
    $pen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $pen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    $g.DrawLine($pen, $x1, $y1, $x2, $y2)
}

$g.Clear([System.Drawing.ColorTranslator]::FromHtml("#F7F8F5"))
FillRR 18 18 354 808 28 "#FAFBF8"
StrokeRR 18 18 354 808 28 "#DDE4DA" 1

Txt "9:41" 34 44 13 "#222B26" ([System.Drawing.FontStyle]::Bold)
StrokeRR 304 48 21 10 3 "#222B26" 1.4
FillRR 327 51 3 4 1 "#222B26"
FillRR 308 51 13 4 2 "#222B26"

Txt "今天继续" 34 82 14 "#59645E" ([System.Drawing.FontStyle]::Bold)
Txt "Java 面试题" 34 108 28 "#17221C" ([System.Drawing.FontStyle]::Bold)
FillRR 300 88 42 42 12 "#EAF2EC"
StrokeRR 300 88 42 42 12 "#D7E4D8" 1
L 313 109 329 109 "#2F6B5B" 2.2
L 321 101 321 117 "#2F6B5B" 2.2

FillRR 34 152 322 154 18 "#214D43"
$accent = [System.Drawing.Drawing2D.GraphicsPath]::new()
$accent.AddLine(292, 152, 338, 152)
$accent.AddLine(356, 170, 356, 216)
$accent.AddLine(331, 212, 307, 190)
$accent.CloseFigure()
$g.FillPath((Brush "#E8B44F"), $accent)
Txt "学习进度" 54 176 14 "#EAF5EF" ([System.Drawing.FontStyle]::Bold)
Txt "42%" 54 207 38 "#FFFFFF" ([System.Drawing.FontStyle]::Bold)
FillRR 54 238 148 24 12 "#2B6256"
Txt "已练 126 / 300 题" 70 240 14 "#FFFFFF" ([System.Drawing.FontStyle]::Bold)
FillRR 54 266 132 24 12 "#EAF2EC"
Txt "继续上次练习" 74 269 12 "#214D43" ([System.Drawing.FontStyle]::Bold)
$g.DrawEllipse((PenC "#3D7468" 12), 247, 194, 76, 76)
$g.DrawArc((PenC "#F1C764" 12), 247, 194, 76, 76, -90, 78)
Txt "18" 269 223 17 "#FFFFFF" ([System.Drawing.FontStyle]::Bold)
Txt "天连续" 263 243 11 "#DDECE5" ([System.Drawing.FontStyle]::Bold)

FillRR 34 322 153 64 14 "#FFFFFF"; StrokeRR 34 322 153 64 14 "#DDE4DA"
FillRR 50 340 30 30 9 "#FFF4D8"
L 62 356 68 356 "#C78A11" 2; L 65 348 65 364 "#C78A11" 2
Txt "今日目标" 92 341 12 "#67736B" ([System.Drawing.FontStyle]::Bold)
Txt "20 题" 92 360 18 "#17221C" ([System.Drawing.FontStyle]::Bold)
FillRR 203 322 153 64 14 "#FFFFFF"; StrokeRR 203 322 153 64 14 "#DDE4DA"
FillRR 219 340 30 30 9 "#FFE9E3"
L 228 356 241 356 "#B94F3E" 2.2; L 234 349 234 363 "#B94F3E" 2.2
Txt "待复盘" 261 341 12 "#67736B" ([System.Drawing.FontStyle]::Bold)
Txt "7 道" 261 360 18 "#17221C" ([System.Drawing.FontStyle]::Bold)

Txt "学习路径" 34 410 19 "#17221C" ([System.Drawing.FontStyle]::Bold)
Txt "优先补薄弱点，再进入综合练习" 34 435 13 "#67736B"

FillRR 34 464 322 78 16 "#FFFFFF"; StrokeRR 34 464 322 78 16 "#DDE4DA"
FillRR 50 482 42 42 12 "#EAF2EC"
L 62 496 80 496 "#2F6B5B" 2.2; L 62 504 80 504 "#2F6B5B" 2.2; L 62 512 73 512 "#2F6B5B" 2.2
Txt "集合与泛型" 108 484 15 "#17221C" ([System.Drawing.FontStyle]::Bold)
Txt "正确率 54% · 12 道相关题" 108 509 12 "#67736B"
FillRR 277 489 56 28 14 "#EAF2EC"; StrokeRR 277 489 56 28 14 "#AFC8BD"
Txt "练习" 294 494 12 "#214D43" ([System.Drawing.FontStyle]::Bold)

FillRR 34 554 322 78 16 "#FFFFFF"; StrokeRR 34 554 322 78 16 "#DDE4DA"
FillRR 50 572 42 42 12 "#FFF4D8"
L 61 592 84 592 "#A66E05" 2.1; L 61 603 84 603 "#A66E05" 2.1
Txt "JVM 内存模型" 108 574 15 "#17221C" ([System.Drawing.FontStyle]::Bold)
Txt "正确率 61% · 推荐复盘" 108 599 12 "#67736B"
FillRR 277 579 56 28 14 "#EEF2ED"; StrokeRR 277 579 56 28 14 "#DDE4DA"
StrokeRR 277 579 56 28 14 "#AFC8BD"
Txt "复盘" 294 584 12 "#214D43" ([System.Drawing.FontStyle]::Bold)

Txt "今日行动" 34 656 19 "#17221C" ([System.Drawing.FontStyle]::Bold)
Txt "只保留不会和底部导航重复的即时操作" 34 679 13 "#67736B"
FillRR 34 696 322 62 16 "#FFFFFF"; StrokeRR 34 696 322 62 16 "#DDE4DA"
FillRR 50 711 136 32 16 "#214D43"
L 70 727 92 727 "#FFFFFF" 2.3; L 81 716 81 738 "#FFFFFF" 2.3
Txt "顺序练习" 102 718 13 "#FFFFFF" ([System.Drawing.FontStyle]::Bold)
StrokeRR 205 711 118 32 16 "#AFC8BD"
$g.DrawEllipse((PenC "#2F6B5B" 2), 220, 718, 14, 14); L 232 730 238 736 "#2F6B5B" 2
Txt "搜索题目" 248 718 13 "#214D43" ([System.Drawing.FontStyle]::Bold)

FillRR 34 778 322 52 22 "#FFFFFF"; StrokeRR 34 778 322 52 22 "#DDE4DA"
FillRR 48 788 70 32 16 "#EAF2EC"
L 74 805 83 797 "#214D43" 2; L 83 797 92 805 "#214D43" 2; L 76 805 76 812 "#214D43" 2; L 92 805 92 812 "#214D43" 2; L 76 812 92 812 "#214D43" 2
Txt "题库" 128 797 12 "#67736B" ([System.Drawing.FontStyle]::Bold)
L 205 797 225 797 "#7C887F" 2; L 205 805 225 805 "#7C887F" 2; L 205 813 217 813 "#7C887F" 2
Txt "复盘" 244 797 12 "#67736B" ([System.Drawing.FontStyle]::Bold)
$g.DrawEllipse((PenC "#7C887F" 2), 310, 795, 16, 16)
L 305 817 331 817 "#7C887F" 2

$bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose()
$bmp.Dispose()
