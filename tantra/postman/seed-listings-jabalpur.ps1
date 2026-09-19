# seed-listings-jabalpur.ps1
# Creates 5 sample listings per category + 5 business profiles, all with Jabalpur location.
# Prereq: server running; catalog already seeded via seed-all.ps1
#
# Usage:  .\seed-listings-jabalpur.ps1
#         .\seed-listings-jabalpur.ps1 http://192.168.1.5:8080

param([string]$BaseUrl = "http://localhost:8080")

$ErrorActionPreference = "Stop"

# ---- helpers ----------------------------------------------------------------

function Invoke-Post($path, $body, $token) {
    $h = @{ "Content-Type" = "application/json" }
    if ($token) { $h["Authorization"] = "Bearer $token" }
    try {
        $r = Invoke-RestMethod -Method POST -Uri "$BaseUrl$path" -Headers $h -Body ($body | ConvertTo-Json -Depth 10)
        if ($r.data -ne $null) { return $r.data } else { return $r }
    } catch {
        $msg = $_.ErrorDetails.Message
        if (-not $msg) { $msg = $_.Exception.Message }
        Write-Host "  WARN POST $path => $msg" -ForegroundColor Yellow
        return $null
    }
}

function Invoke-Get($path, $token) {
    $h = @{}
    if ($token) { $h["Authorization"] = "Bearer $token" }
    try {
        $r = Invoke-RestMethod -Method GET -Uri "$BaseUrl$path" -Headers $h
        if ($r.data -ne $null) { return $r.data } else { return $r }
    } catch {
        $msg = $_.ErrorDetails.Message
        if (-not $msg) { $msg = $_.Exception.Message }
        Write-Host "  WARN GET $path => $msg" -ForegroundColor Yellow
        return $null
    }
}

function Banner($msg) {
    Write-Host ""
    Write-Host "-- $msg" -ForegroundColor Cyan
}

# ---- shared address (Jabalpur, MP) ------------------------------------------

$Addr = @{
    fullAddress  = "Near Mela Ground, Jabalpur"
    country      = "IN"
    state        = "MP"
    district     = "Jabalpur"
    city         = "Jabalpur"
    village      = "Jabalpur"
    pinCode      = "482001"
    latitude     = 23.1815
    longitude    = 79.9864
    mobileNumber = "9000000002"
}

$Imgs = @("https://cdn.tantra.app/seed/a.jpg", "https://cdn.tantra.app/seed/b.jpg")

# ---- 1. Auth ----------------------------------------------------------------

Banner "Auth"

Invoke-Post "/api/v1/auth/signup" @{
    firstName = "Admin"; lastName = "Tantra"
    mobileNumber = "9000000001"; password = "Admin@123"
    appUsageRole = "ADMIN"; preferredLanguage = "EN"
} $null | Out-Null

Invoke-Post "/api/v1/auth/signup" @{
    firstName = "Ravi"; lastName = "Kumar"
    mobileNumber = "9000000002"; password = "Seller@123"
    appUsageRole = "USER"; preferredLanguage = "HI"
} $null | Out-Null

$adminAuth  = Invoke-Post "/api/v1/auth/signin" @{ mobileNumber = "9000000001"; password = "Admin@123" } $null
$AdminToken = $adminAuth.token
Write-Host "  Admin signed in: token=$($AdminToken.Substring(0,[Math]::Min(20,$AdminToken.Length)))..."

$sellerAuth  = Invoke-Post "/api/v1/auth/signin" @{ mobileNumber = "9000000002"; password = "Seller@123" } $null
$SellerToken = $sellerAuth.token
Write-Host "  Seller signed in: token=$($SellerToken.Substring(0,[Math]::Min(20,$SellerToken.Length)))..."

# ---- 2. Discover module & category IDs --------------------------------------

Banner "Discovering module & category IDs"

$modules     = Invoke-Get "/api/v1/masters/modules" $null
$agriMod     = $modules | Where-Object { $_.moduleKey -eq "agriculture" }     | Select-Object -First 1
$animalMod   = $modules | Where-Object { $_.moduleKey -eq "animal_livestock" }| Select-Object -First 1

if (-not $agriMod)   { throw "agriculture module not found. Run seed-all.ps1 first." }
if (-not $animalMod) { throw "animal_livestock module not found. Run seed-all.ps1 first." }

$AgriId   = $agriMod.id
$AnimalId = $animalMod.id
Write-Host "  agriculture id=$AgriId   animal_livestock id=$AnimalId"

$agriTopCats   = Invoke-Get "/api/v1/masters/modules/$AgriId/categories"   $null
$animalTopCats = Invoke-Get "/api/v1/masters/modules/$AnimalId/categories" $null

# agriculture top-level: agri_marketplace, agri_services, repair
$agriMarketplace = $agriTopCats | Where-Object { $_.categoryKey -eq "agri_marketplace" } | Select-Object -First 1
if (-not $agriMarketplace) { throw "agri_marketplace category not found. Run seed-all.ps1 first." }

# subcategories of agri_marketplace
$marketplaceSubs = Invoke-Get "/api/v1/masters/categories/$($agriMarketplace.id)/subcategories" $null

function Find-In($cats, $key) {
    return ($cats | Where-Object { $_.categoryKey -eq $key } | Select-Object -First 1)
}

$CatCrop       = Find-In $marketplaceSubs "crop"
$CatSeed       = Find-In $marketplaceSubs "seed"
$CatFertilizer = Find-In $marketplaceSubs "fertilizer"
$CatPesticide  = Find-In $marketplaceSubs "pesticide"
$CatEquipment  = Find-In $marketplaceSubs "equipment"
$CatRepair       = Find-In $agriTopCats     "repair"

$animalMarketplace = $animalTopCats | Where-Object { $_.categoryKey -eq "animal_marketplace" } | Select-Object -First 1
if (-not $animalMarketplace) { throw "animal_marketplace category not found. Run seed-all.ps1 first." }
$animalSubs = Invoke-Get "/api/v1/masters/categories/$($animalMarketplace.id)/subcategories" $null
$CatAnimal   = Find-In $animalSubs "animal"

foreach ($pair in @(
    [pscustomobject]@{n="crop";       c=$CatCrop},
    [pscustomobject]@{n="seed";       c=$CatSeed},
    [pscustomobject]@{n="fertilizer"; c=$CatFertilizer},
    [pscustomobject]@{n="pesticide";  c=$CatPesticide},
    [pscustomobject]@{n="equipment";  c=$CatEquipment},
    [pscustomobject]@{n="repair";     c=$CatRepair},
    [pscustomobject]@{n="animal";     c=$CatAnimal}
)) {
    if (-not $pair.c) { throw "$($pair.n) category not found in DB. Confirm seed-all.ps1 ran." }
    Write-Host "  $($pair.n) => id=$($pair.c.id)"
}

# ---- 3. Listing create helper -----------------------------------------------

$TotalCreated = 0

function New-Listing($catId, $modId, $listType, $title, $actualPrice, $offeredPrice, $qty, $unit, $attrs) {
    $body = @{
        categoryId        = $catId
        moduleId          = $modId
        listingType       = $listType
        listingTitle      = $title
        actualPrice       = $actualPrice
        offeredPrice      = $offeredPrice
        quantity          = $qty
        unit              = $unit
        isNegotiable      = $true
        contactNumber     = "9000000002"
        showContact       = $true
        useDefaultAddress = $false
        images            = $script:Imgs
        address           = $script:Addr
        attributes        = $attrs
    }
    $r = Invoke-Post "/api/v1/listings" $body $script:SellerToken
    if ($r -and $r.listingId) {
        Write-Host "    OK  $($r.listingId) - $title"
        $script:TotalCreated++
    } else {
        Write-Host "    FAIL  $title" -ForegroundColor Yellow
    }
}

# ---- 4. CROP listings -------------------------------------------------------

Banner "Crop listings (5)"

New-Listing $CatCrop.id $AgriId "SELL" "Premium Wheat - Grade A" 2800 2600 50 "quintal" @{
    cropType="cereal"; cropName="wheat"; qualityGrade="A"
    harvestYear="2026"; harvestMonth="02"
    qty=50; qtyMeasurement="quintal"
    cropVariety="GW-322"; cropDescription="Good quality wheat from Jabalpur farm"
}
New-Listing $CatCrop.id $AgriId "SELL" "Fresh Paddy - Kharif 2025" 1800 1650 100 "quintal" @{
    cropType="cereal"; cropName="paddy"; qualityGrade="B"
    harvestYear="2025"; harvestMonth="10"
    qty=100; qtyMeasurement="quintal"
    cropVariety="IR-64"; cropDescription="Freshly harvested paddy, low moisture"
}
New-Listing $CatCrop.id $AgriId "SELL" "Mustard Oilseed - MP Grade A" 5200 4900 30 "quintal" @{
    cropType="oilseed"; cropName="mustard"; qualityGrade="A"
    harvestYear="2026"; harvestMonth="01"
    qty=30; qtyMeasurement="quintal"
    cropVariety="Pusa Bold"; cropDescription="High-oil-content mustard from Jabalpur"
}
New-Listing $CatCrop.id $AgriId "SELL" "Soybean - Good Grade JS-335" 4000 3800 80 "quintal" @{
    cropType="oilseed"; cropName="soybean"; qualityGrade="B"
    harvestYear="2025"; harvestMonth="09"
    qty=80; qtyMeasurement="quintal"
    cropVariety="JS-335"; cropDescription="Cleaned and graded soybean"
}
New-Listing $CatCrop.id $AgriId "SELL" "Chana Dal Lot - JG-315" 5800 5500 20 "quintal" @{
    cropType="pulse"; cropName="gram"; qualityGrade="A"
    harvestYear="2026"; harvestMonth="03"
    qty=20; qtyMeasurement="quintal"
    cropVariety="JG-315"; cropDescription="Bold-grain chickpea from Jabalpur district"
}

# ---- 5. SEED listings -------------------------------------------------------

Banner "Seed listings (5)"

New-Listing $CatSeed.id $AgriId "SELL" "Paddy Seed IR-64 Certified" 350 320 25 "kg" @{
    seedType="cereal"; seedName="cereal_paddy"
    seedBrand="IFFCO Seeds"; seedVariety="IR-64"
    seedDescription="Certified paddy seed 85 percent germination"
    qty=25; qtyMeasurement="kg"; packOf=1
}
New-Listing $CatSeed.id $AgriId "SELL" "Wheat Seed GW-322" 280 260 20 "kg" @{
    seedType="cereal"; seedName="cereal_wheat"
    seedBrand="MP Agro"; seedVariety="GW-322"
    seedDescription="Disease-resistant wheat seed"
    qty=20; qtyMeasurement="kg"; packOf=1
}
New-Listing $CatSeed.id $AgriId "SELL" "Soybean Seed JS-335" 400 380 30 "kg" @{
    seedType="oilseed"; seedName="oilseed_soybean"
    seedBrand="MPKV"; seedVariety="JS-335"
    seedDescription="High-yield soybean variety"
    qty=30; qtyMeasurement="kg"; packOf=1
}
New-Listing $CatSeed.id $AgriId "SELL" "Chana Seed Desi JG-315" 180 165 10 "kg" @{
    seedType="pulse"; seedName="pulse_gram"
    seedBrand="Pioneer"; seedVariety="JG-315"
    seedDescription="Bold desi chana seed lot"
    qty=10; qtyMeasurement="kg"; packOf=1
}
New-Listing $CatSeed.id $AgriId "SELL" "Mustard Seed Pusa Bold" 250 230 5 "kg" @{
    seedType="oilseed"; seedName="oilseed_mustard"
    seedBrand="Syngenta"; seedVariety="Pusa Bold"
    seedDescription="High-oil mustard seed"
    qty=5; qtyMeasurement="kg"; packOf=1
}

# ---- 6. FERTILIZER listings -------------------------------------------------

Banner "Fertilizer listings (5)"

New-Listing $CatFertilizer.id $AgriId "SELL" "Urea 46 Percent N - 50 kg Bag" 280 260 10 "kg" @{
    fertilizerCategory="nitrogen"; fertilizerName="urea"
    fertilizerType="granule"; fertilizerBrand="IFFCO"
    fertilizerDescription="Standard urea 50 kg bag"
    qty=10; qtyMeasurement="kg"; packOf=50
}
New-Listing $CatFertilizer.id $AgriId "SELL" "DAP Fertilizer 50 kg" 1400 1350 10 "kg" @{
    fertilizerCategory="phosphorus"; fertilizerName="dap"
    fertilizerType="granule"; fertilizerBrand="Coromandel"
    fertilizerDescription="Diammonium phosphate 18-46-0"
    qty=10; qtyMeasurement="kg"; packOf=50
}
New-Listing $CatFertilizer.id $AgriId "SELL" "MOP Potash Fertilizer 25 kg" 900 870 5 "kg" @{
    fertilizerCategory="potassium"; fertilizerName="mop"
    fertilizerType="granule"; fertilizerBrand="IPL"
    fertilizerDescription="Muriate of potash for all crops"
    qty=5; qtyMeasurement="kg"; packOf=25
}
New-Listing $CatFertilizer.id $AgriId "SELL" "NPK 12-32-16 Complex" 1200 1150 20 "kg" @{
    fertilizerCategory="complex"; fertilizerName="npk_12_32"
    fertilizerType="granule"; fertilizerBrand="Tata"
    fertilizerDescription="Complex fertilizer for kharif crops"
    qty=20; qtyMeasurement="kg"; packOf=50
}
New-Listing $CatFertilizer.id $AgriId "SELL" "Vermi Compost Organic" 120 110 50 "kg" @{
    fertilizerCategory="organic"; fertilizerName="vermicompost"
    fertilizerType="powder"; fertilizerBrand="KisanCare"
    fertilizerDescription="Ready-to-use vermi compost"
    qty=50; qtyMeasurement="kg"; packOf=10
}

# ---- 7. PESTICIDE listings --------------------------------------------------

Banner "Pesticide listings (5)"

New-Listing $CatPesticide.id $AgriId "SELL" "Chlorpyrifos Insecticide 1L" 450 420 10 "kg" @{
    pesticideCategory="insecticide"; pesticideName="Chlorpyrifos"
    pesticideType="liquid"; pesticideBrand="Dhanuka"
    pesticideUsage="Dilute in water"; pesticideUsedFor="Paddy and Soybean"
    pesticideDescription="Broad-spectrum insecticide 50 EC"
    qty=10; qtyMeasurement="kg"; packOf=1
}
New-Listing $CatPesticide.id $AgriId "SELL" "Mancozeb Fungicide 500g" 280 260 20 "kg" @{
    pesticideCategory="fungicide"; pesticideName="Mancozeb"
    pesticideType="powder"; pesticideBrand="BASF"
    pesticideUsage="Foliar spray"; pesticideUsedFor="Wheat and Vegetables"
    pesticideDescription="Contact fungicide for foliar diseases"
    qty=20; qtyMeasurement="kg"; packOf=1
}
New-Listing $CatPesticide.id $AgriId "SELL" "Glyphosate Herbicide 1L" 380 360 15 "kg" @{
    pesticideCategory="herbicide"; pesticideName="Glyphosate"
    pesticideType="liquid"; pesticideBrand="Monsanto"
    pesticideUsage="Direct soil spray"; pesticideUsedFor="Weeds"
    pesticideDescription="Non-selective post-emergence herbicide"
    qty=15; qtyMeasurement="kg"; packOf=1
}
New-Listing $CatPesticide.id $AgriId "SELL" "Imidacloprid Systemic 100ml" 320 300 5 "kg" @{
    pesticideCategory="insecticide"; pesticideName="Imidacloprid"
    pesticideType="liquid"; pesticideBrand="Bayer"
    pesticideUsage="Seed treatment or Spray"; pesticideUsedFor="Cotton and Soybean"
    pesticideDescription="Systemic insecticide 17.8 SL"
    qty=5; qtyMeasurement="kg"; packOf=1
}
New-Listing $CatPesticide.id $AgriId "SELL" "Carbendazim Fungicide 250g" 210 195 10 "kg" @{
    pesticideCategory="fungicide"; pesticideName="Carbendazim"
    pesticideType="powder"; pesticideBrand="UPL"
    pesticideUsage="Foliar spray"; pesticideUsedFor="All crops"
    pesticideDescription="Systemic fungicide 50 WP"
    qty=10; qtyMeasurement="kg"; packOf=1
}

# ---- 8. ANIMAL listings -----------------------------------------------------

Banner "Animal listings (5)"

New-Listing $CatAnimal.id $AnimalId "SELL" "Murrah Buffalo Dairy" 55000 52000 1 "head" @{
    animalCategory="cattle_buffalo"; animalName="buffalo"
    breed="Murrah"; gender="female"; age=4; ageUnit="years"; weight=450
    purpose="dairy"; healthStatus=@("healthy","vaccinated"); availableQty=1
    animalDescription="High-milk Murrah buffalo 12L per day Jabalpur"
}
New-Listing $CatAnimal.id $AnimalId "SELL" "HF Cross Cow 15L Milk" 45000 42000 2 "head" @{
    animalCategory="cattle_buffalo"; animalName="cow"
    breed="HF Cross"; gender="female"; age=3; ageUnit="years"; weight=380
    purpose="dairy"; healthStatus=@("healthy","vaccinated"); availableQty=2
    animalDescription="High-yield HF cross breed 15 litres per day"
}
New-Listing $CatAnimal.id $AnimalId "SELL" "Sirohi Goat Pair" 8000 7500 2 "head" @{
    animalCategory="goat_sheep"; animalName="goat"
    breed="Sirohi"; gender="male"; age=18; ageUnit="months"; weight=40
    purpose="meat"; healthStatus=@("healthy"); availableQty=2
    animalDescription="Sirohi breed goat pair well maintained"
}
New-Listing $CatAnimal.id $AnimalId "SELL" "Nali Sheep Meat Breed" 6500 6000 3 "head" @{
    animalCategory="goat_sheep"; animalName="sheep"
    breed="Nali"; gender="male"; age=2; ageUnit="years"; weight=55
    purpose="meat"; healthStatus=@("healthy","dewormed"); availableQty=3
    animalDescription="Adult Nali sheep ready for sale"
}
New-Listing $CatAnimal.id $AnimalId "SELL" "Country Pig Breeding Female" 9000 8500 1 "head" @{
    animalCategory="pig"; animalName="pig"
    breed="Desi"; gender="female"; age=12; ageUnit="months"; weight=80
    purpose="breeding"; healthStatus=@("healthy","vaccinated"); availableQty=1
    animalDescription="Healthy country pig for breeding purpose"
}

# ---- 9. REPAIR & MAINTENANCE listings ---------------------------------------

Banner "Repair and Maintenance listings (5)"

New-Listing $CatRepair.id $AgriId "SELL" "Tractor Repair All Brands" 500 500 1 "service" @{
    repairCategory="machinery"; repairType="tractor"
    description="Expert tractor repair and overhauling all brands"
    expertise="Tractor engine and hydraulics"; experienceYears=10; visitCharge=500
    isNegotiable=$true
}
New-Listing $CatRepair.id $AgriId "SELL" "Water Pump Repair and Service" 300 300 1 "service" @{
    repairCategory="pump_motor"; repairType="water_pump"
    description="Surface and submersible pump repair service"
    expertise="Pump and motor winding"; experienceYears=8; visitCharge=300
    isNegotiable=$true
}
New-Listing $CatRepair.id $AgriId "SELL" "Harvester and Thresher Servicing" 800 800 1 "service" @{
    repairCategory="machinery"; repairType="harvester"
    description="Combine harvester and thresher repair before season"
    expertise="Combine harvester machinery"; experienceYears=12; visitCharge=800
    isNegotiable=$true
}
New-Listing $CatRepair.id $AgriId "SELL" "Solar Panel Repair Farm" 400 400 1 "service" @{
    repairCategory="electrical"; repairType="solar_panel"
    description="Solar panel cleaning wiring and inverter repair"
    expertise="Solar PV systems"; experienceYears=5; visitCharge=400
    isNegotiable=$true
}
New-Listing $CatRepair.id $AgriId "SELL" "Drip Sprinkler System Repair" 350 350 1 "service" @{
    repairCategory="irrigation"; repairType="drip_repair"
    description="Drip line unclogging emitter replacement pipe fix"
    expertise="Drip and sprinkler irrigation"; experienceYears=7; visitCharge=350
    isNegotiable=$true
}

# ---- 10. EQUIPMENT - RENT listings ------------------------------------------

Banner "Agriculture Equipment - RENT (5)"

New-Listing $CatEquipment.id $AgriId "RENT" "Tractor on Rent 45 HP" 350 350 1 "hour" @{
    equipmentCategory="tractor"; equipmentName="tractor"
    availableFor="rent"; rentPerHour=350; availableQty=1
}
New-Listing $CatEquipment.id $AgriId "RENT" "Rotavator on Rent" 200 200 1 "hour" @{
    equipmentCategory="tractor"; equipmentName="rotavator"
    availableFor="rent"; rentPerHour=200; availableQty=2
}
New-Listing $CatEquipment.id $AgriId "RENT" "Combine Harvester Seasonal Rent" 1200 1200 1 "hour" @{
    equipmentCategory="harvester"; equipmentName="harvester"
    availableFor="rent"; rentPerHour=1200; availableQty=1
}
New-Listing $CatEquipment.id $AgriId "RENT" "Thresher for Rent Per Hour" 250 250 1 "hour" @{
    equipmentCategory="thresher"; equipmentName="thresher"
    availableFor="rent"; rentPerHour=250; availableQty=1
}
New-Listing $CatEquipment.id $AgriId "RENT" "Power Tiller on Rent" 150 150 1 "hour" @{
    equipmentCategory="tractor"; equipmentName="power_tiller"
    availableFor="rent"; rentPerHour=150; availableQty=2
}

# ---- 11. EQUIPMENT - SELL listings ------------------------------------------

Banner "Agriculture Equipment - SELL (5)"

New-Listing $CatEquipment.id $AgriId "SELL" "Used Tractor Mahindra 475" 450000 420000 1 "piece" @{
    equipmentCategory="tractor"; equipmentName="tractor"
    availableFor="sell"; brandName="Mahindra"; modelNumber="475 DI"; modelYear=2019; availableQty=1
}
New-Listing $CatEquipment.id $AgriId "SELL" "Rotavator New 5 ft Fieldking" 85000 80000 1 "piece" @{
    equipmentCategory="tractor"; equipmentName="rotavator"
    availableFor="sell"; brandName="Fieldking"; modelNumber="RT-160"; modelYear=2024; availableQty=1
}
New-Listing $CatEquipment.id $AgriId "SELL" "Sprayer Pump Knapsack Neptune" 1800 1650 5 "piece" @{
    equipmentCategory="sprayer"; equipmentName="sprayer"
    availableFor="sell"; brandName="Neptune"; modelNumber="KS-16"; modelYear=2023; availableQty=5
}
New-Listing $CatEquipment.id $AgriId "SELL" "Seed Drill 9-Row Shaktiman" 75000 70000 1 "piece" @{
    equipmentCategory="seed_drill"; equipmentName="seed_drill"
    availableFor="sell"; brandName="Shaktiman"; modelNumber="SD-9"; modelYear=2023; availableQty=1
}
New-Listing $CatEquipment.id $AgriId "SELL" "Harvester Kartar 4000 Used" 1200000 1100000 1 "piece" @{
    equipmentCategory="harvester"; equipmentName="harvester"
    availableFor="sell"; brandName="Kartar"; modelNumber="4000"; modelYear=2018; availableQty=1
}

# ---- 12. BUSINESS PROFILES --------------------------------------------------

Banner "Business Profiles (5)"

$BpCreated = 0

$BpAddr = @{
    fullAddress  = "Shop Near Kalyan Market, Jabalpur"
    country      = "IN"
    state        = "MP"
    district     = "Jabalpur"
    city         = "Jabalpur"
    pinCode      = "482001"
    mobileNumber = "9000000002"
}

function New-BusinessProfile($profileType, $businessName, $ownerName, $email, $gst, $desc) {
    $body = @{
        profileType  = $profileType
        businessName = $businessName
        isVisible    = $true
        address      = $script:BpAddr
        attributes   = @{
            ownerName    = $ownerName
            description  = $desc
            mobileNumber = "9000000002"
            email        = $email
            gstNumber    = $gst
        }
    }
    $r = Invoke-Post "/api/v1/business-profiles" $body $script:SellerToken
    if ($r -and $r.profileId) {
        Write-Host "    OK  $($r.profileId) - $businessName"
        $script:BpCreated++
    } else {
        Write-Host "    FAIL  $businessName" -ForegroundColor Yellow
    }
}

New-BusinessProfile "seed_dealer"      "Jabalpur Seed House"          "Suresh Patel"    "suresh@jabalpurseed.com" "23ABCPS1234F1Z5" "Certified seed dealer for MP region"
New-BusinessProfile "fertilizer_shop"  "Kisan Fertilizer Store"       "Ramesh Gupta"    "ramesh@kisanfert.com"    "23ABCPG5678F1Z3" "All types of fertilizers at best price"
New-BusinessProfile "pesticide_dealer" "Agro Pesticide Depot"         "Priya Singh"     "priya@agropest.com"      "23ABCRS9012F1Z1" "Licensed pesticide dealer Jabalpur"
New-BusinessProfile "equipment_dealer" "MP Agri Equipment Center"     "Vijay Kumar"     "vijay@mpagrieq.com"      "23ABCRV3456F1Z9" "Farm equipment sales and service center"
New-BusinessProfile "vet_clinic"       "Pashu Chikitsalaya Jabalpur"  "Dr Anand Misra"  "anand.vet@gmail.com"     "23ABCAM7890F1Z7" "Veterinary clinic for all animals 10 yrs experience"

# ---- Summary ----------------------------------------------------------------

Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host " Done. $TotalCreated listings + $BpCreated business profiles created." -ForegroundColor Green
Write-Host " Location: Jabalpur, MP - 482001" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
