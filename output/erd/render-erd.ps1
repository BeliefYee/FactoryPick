param([string]$DataPath,[string]$OutputDir)
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$data=Get-Content -LiteralPath $DataPath -Raw -Encoding UTF8 | ConvertFrom-Json
$fontName='맑은 고딕'
function Brush([string]$hex) { return [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml($hex)) }
function Text([string]$value,[float]$x,[float]$y,[float]$size=13,[string]$color='#283e56',[bool]$bold=$false) {
  $style=[System.Drawing.FontStyle]::Regular
  if($bold){$style=[System.Drawing.FontStyle]::Bold}
  $font=[System.Drawing.Font]::new($fontName,$size,$style,[System.Drawing.GraphicsUnit]::Pixel)
  $brush=Brush $color
  $g.DrawString($value,$font,$brush,$x,$y)
  $font.Dispose();$brush.Dispose()
}
function Rect([float]$x,[float]$y,[float]$w,[float]$h,[string]$fill,[string]$stroke='') {
  $brush=Brush $fill;$g.FillRectangle($brush,$x,$y,$w,$h);$brush.Dispose()
  if($stroke){$pen=[System.Drawing.Pen]::new([System.Drawing.ColorTranslator]::FromHtml($stroke),1);$g.DrawRectangle($pen,$x,$y,$w,$h);$pen.Dispose()}
}
function Link($coords,[string]$color,[string]$label,[float]$tx,[float]$ty,[bool]$dash=$false){
  $pen=[System.Drawing.Pen]::new([System.Drawing.ColorTranslator]::FromHtml($color),2)
  if($dash){$pen.DashStyle=[System.Drawing.Drawing2D.DashStyle]::Dash}
  $pts=@();for($i=0;$i -lt $coords.Length;$i+=2){$pts += [System.Drawing.PointF]::new($coords[$i],$coords[$i+1])}
  $g.DrawLines($pen,[System.Drawing.PointF[]]$pts)
  $end=$pts[-1];$prev=$pts[-2];$dx=$end.X-$prev.X;$dy=$end.Y-$prev.Y;$len=[Math]::Sqrt($dx*$dx+$dy*$dy);$ux=$dx/$len;$uy=$dy/$len
  $arrow=[System.Drawing.PointF[]]@($end,[System.Drawing.PointF]::new($end.X-10*$ux+4*$uy,$end.Y-10*$uy-4*$ux),[System.Drawing.PointF]::new($end.X-10*$ux-4*$uy,$end.Y-10*$uy+4*$ux))
  $b=Brush $color;$g.FillPolygon($b,$arrow);$b.Dispose();$pen.Dispose()
  Text $label $tx $ty 12 $color $true
}
function Card([string]$name,[float]$x,[float]$y,[float]$w,[string]$subtitle,[int]$rowHeight=24,[float]$fontSize=12.5){
  $rows=$data.tables.$name;$h=90+$rows.Count*$rowHeight+8
  $accent='#2467a2';if($name -eq 'factory'){$accent='#124a7c'};if($name -eq 'factory_category'){$accent='#7152a3'}
  Rect $x $y $w $h '#ffffff' '#b8cbdc';Rect $x $y $w 60 $accent
  Text $name ($x+14) ($y+8) 18 '#ffffff' $true
  Text $subtitle ($x+14) ($y+34) 11 '#e0edf8'
  Rect $x ($y+60) $w 30 '#eaf1f8'
  $typeX=$x+$w-305;$keyX=$x+$w-175;$nullX=$x+$w-108
  Text 'COLUMN' ($x+14) ($y+67) 11 '#627b91' $true
  Text 'TYPE' $typeX ($y+67) 11 '#627b91' $true
  Text 'KEY' $keyX ($y+67) 11 '#627b91' $true
  Text 'NULL / NOTE' $nullX ($y+67) 11 '#627b91' $true
  $i=0;foreach($row in $rows){$ry=$y+90+$i*$rowHeight;if($i%2 -eq 1){Rect ($x+1) $ry ($w-2) $rowHeight '#f5f8fc'}
    Text $row.name ($x+14) ($ry+4) $fontSize '#253b50' ($row.key -match 'PK')
    Text $row.type $typeX ($ry+4) ($fontSize-0.5) '#536d83'
    Text $row.key $keyX ($ry+4) ($fontSize-0.5) '#1766a8' $true
    Text $row.nullable $nullX ($ry+4) ($fontSize-0.5) '#536d83'
    $i++
  }
}
function StartImage([int]$w,[int]$h){
  $script:bitmap=[System.Drawing.Bitmap]::new($w*2,$h*2)
  $bitmap.SetResolution(192,192)
  $script:g=[System.Drawing.Graphics]::FromImage($bitmap)
  $g.Clear([System.Drawing.ColorTranslator]::FromHtml('#f3f6fb'))
  $g.ScaleTransform(2,2)
  $g.SmoothingMode=[System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $g.TextRenderingHint=[System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
}
function SaveImage([string]$name){$bitmap.Save((Join-Path $OutputDir $name),[System.Drawing.Imaging.ImageFormat]::Png);$g.Dispose();$bitmap.Dispose()}
StartImage 2400 1700
Text 'FactoryPick  |  전체 ERD' 60 38 36 '#173b5b' $true
Text '물리 테이블 10개 + 조회용 VIEW 1개 · 모든 컬럼 표시 · PK / FK / NULL 구분' 62 94 17 '#577089'
Text '기준: 저장소 schema.sql + category-schema.sql / 운영 DB 직접 조회 결과는 아님' 62 126 14 '#6b8094'
# Solid arrows: child foreign key -> referenced parent key. Labels list parent:child cardinality.
Link @(850,404,750,404,750,260,650,260) '#2675b6' 'company_id  |  0..1 : 0..N' 665 335
Link @(850,428,720,428,720,500,650,500) '#2675b6' 'region_id  |  0..1 : 0..N' 665 540
Link @(650,720,800,720,800,292,850,292) '#2675b6' 'factory_id  |  1 : 0..N' 665 760
Link @(650,970,700,970,700,1055,1050,1055,1050,960) '#2675b6' 'factory_id  |  1 : 0..N' 765 1020
Link @(355,1046,355,1200) '#2675b6' 'product_id  |  1 : 0..N' 375 1110
Link @(1750,520,1640,520,1640,260,1750,260) '#2675b6' 'category_id  |  1 : 0..N' 1570 370
Link @(650,1350,690,1350,690,1495,2370,1495,2370,260,2340,260) '#2675b6' 'category_id  |  0..1 : 0..N' 1810 1465
Link @(1300,960,1300,1150) '#8563b5' '업종 코드로 계산' 1320 1045 $true
Link @(1750,550,1600,550,1600,1220,1550,1220) '#8563b5' 'VIEW 의존성 (FK 아님)' 1560 1000 $true
foreach($item in $data.layouts){Card $item[0] $item[1] $item[2] $item[3] $item[4]}
Rect 850 1330 700 110 '#ece7f6' '#d5c8e7'
Text 'factory_category VIEW' 870 1344 18 '#65468f' $true
Text 'factory.primary_industry_code → 업종 접두사 → 분류 규칙' 870 1376 14 '#65468f'
Text '기본 앞 2자리, 58로 시작하면 앞 3자리 · 저장 테이블 아님' 870 1404 14 '#65468f'
Rect 60 1535 2280 112 '#ffffff' '#cbd8e5'
Text '표기' 80 1551 17 '#173b5b' $true
Text '실선 화살표: 자식 FK → 부모 PK   |   관계 숫자: 부모 1행과 연결되는 자식의 수 (부모 : 자식)   |   점선: VIEW 계산 의존성' 145 1554 14 '#536d83'
Text 'PK 기본키 · FK 외래키 · UK 단일 UNIQUE · 복합 PK/UNIQUE는 아래 설명 참고' 80 1584 14 '#536d83'
Text '복합 UNIQUE: region(sido_name, sigungu_name), product(product_name, category_key)  /  category_key = COALESCE(category_id, 0)' 80 1612 14 '#536d83'
SaveImage 'FactoryPick-ERD.png'
StartImage 1120 1360
Text 'FactoryPick  |  공장 테이블 상세' 60 34 32 '#173b5b' $true
Text 'factory · 전체 28개 컬럼 · 고해상도 상세 이미지' 62 88 17 '#577089'
Card 'factory' 60 142 1000 '핵심 공장 데이터' 34 17
Text 'company_id → company.company_id  /  region_id → region.region_id' 60 1212 17 '#536d83'
Text '위도·경도는 둘 다 NULL 또는 둘 다 값이 있어야 합니다.' 60 1247 17 '#536d83'
Text 'UK: business_number, factory_manage_no (각각 UNIQUE · NULL 허용)' 60 1282 17 '#536d83'
SaveImage 'FactoryPick-factory-details.png'
Write-Output 'Generated: FactoryPick-ERD.png (4800x3400), FactoryPick-factory-details.png (2240x2720)'
