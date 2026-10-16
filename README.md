# AxSpawnerFarmerHook

**Türkçe** · [**English**](README.en.md)

![Java 21+](https://img.shields.io/badge/Java-21%2B-F4BD60?style=flat-square)
![Paper API 1.21.4](https://img.shields.io/badge/Paper_API-1.21.4-55C7F4?style=flat-square)
[![GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-4ADB9B?style=flat-square)](LICENSE)
[![Son sürüm](https://img.shields.io/github/v/release/SerhatDew/axspawners-uxmfarmer?display_name=tag&style=flat-square&color=F4BD60&label=Son%20s%C3%BCr%C3%BCm)](https://github.com/SerhatDew/axspawners-uxmfarmer/releases/latest)

<p align="center"><img src="assets/hero.svg" alt="AxSpawnerFarmerHook ürün görseli" width="960"></p>

AxSpawners'ın yeni ürettiği eşyaları, spawner'ın bulunduğu bölgedeki uxmFarmer'a aktaran bağımsız Paper eklentisi. AxSpawners veya uxmFarmer üzerinde değişiklik gerektirmez.

[**En yeni JAR'ı indir**](https://github.com/SerhatDew/axspawners-uxmfarmer/releases/latest/download/AxSpawnerFarmerHook.jar) · [Sürüm notları ve SHA-256](https://github.com/SerhatDew/axspawners-uxmfarmer/releases/latest)

![Yeni spawner eşyalarının çiftçiye aktarımı ve kalan miktarın AxSpawners deposuna yönlendirilmesi](assets/collection-flow.gif)

*Temsili işleyiş animasyonu; oyun içi kayıt değildir.*

[İndir](#indir) · [Gereksinimler](#gereksinimler) · [Kurulum](#kurulum) · [Ayar rehberi](#ayar-rehberi) · [İlk test](#ilk-test) · [Derleme](#derleme) · [Toplama kuralları](#toplama-kuralları) · [Sorun giderme](#sorun-giderme)

## Özellikler

- Yeni üretilen AxSpawners eşyalarını spawner bölgesindeki uxmFarmer'a aktarır.
- Ana iş parçacığı handoff'u ve sabitlenmiş sıra miktarıyla çiftçiye sığmayan kısmı AxSpawners deposuna yollar.
- Ayrı ürün yapılandırması tutmadan mevcut AxSpawners ve uxmFarmer kurulumlarına köprü olur.

<a id="indir"></a>

## İndir

Hazır `AxSpawnerFarmerHook.jar` dosyasını [en yeni GitHub Release sayfasından](https://github.com/SerhatDew/axspawners-uxmfarmer/releases/latest) indirin. Kaynaktan derlemiyorsanız **Source code** arşivini değil, sürüm varlıklarındaki JAR'ı kullanın.

## Gereksinimler

- Java 21 veya üzeri.
- Paper; proje 1.21.4 API'sine karşı derlenir. Leaf 26.2 / Java 25 test ağında açılış ve bağımlılık hata akışı gözlendi; uxmFarmer lisansı eksik olduğu için gerçek loot aktarımı doğrulanmadı. Bu gözlem Paper 26.2 veya bütün forklar için tam destek garantisi değildir. Folia desteklenmez.
- AxSpawners 1.12.1 ve geçerli lisansla etkinleşmiş uxmFarmer 3.18.1-SNAPSHOT. Bunlar `depend` bağımlılıklarıdır; JAR'larının klasörde bulunması tek başına yeterli değildir.
- Kaynaktan derlemek için Maven 3.

Java 21 bytecode'u Java 25 ile uyumludur; bu, belirli bir sunucu sürümünün desteklendiği anlamına gelmez.

## Derleme

AxSpawners ve uxmFarmer'ı kendi lisans koşullarına uygun şekilde ayrıca edin. Bu eklentilerin jarları ve kaynakları bu repoda dağıtılmaz.

Jarları `AxSpawners-1.12.1.jar` ve `uxmFarmer-3.18.1-SNAPSHOT.jar` konumlarına koyup yerel Maven deposuna yükle:

```sh
mvn install:install-file -Dfile=AxSpawners-1.12.1.jar -DgroupId=com.artillexstudios -DartifactId=AxSpawners -Dversion=1.12.1 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=uxmFarmer-3.18.1-SNAPSHOT.jar -DgroupId=com.uxplima -DartifactId=uxmFarmer -Dversion=3.18.1-SNAPSHOT -Dpackaging=jar -DgeneratePom=true
mvn clean verify
```

Çıktı: `target/AxSpawnerFarmerHook.jar`. Paper, AxSpawners ve uxmFarmer bağımlılıkları provided kapsamındadır ve jar'a eklenmez.
## Kurulum

1. AxSpawners ve uxmFarmer'ı, kendi gerekli bağımlılıklarıyla birlikte kurup etkinleştir.
2. `AxSpawnerFarmerHook.jar` dosyasını sunucunun `plugins` klasörüne koy.
   Aynı köprünün eski JAR'ını kaldır; eski ve yeni kopyayı birlikte çalıştırma.
3. Sunucuyu yeniden başlat.

Hook, `plugins/AxSpawnerFarmerHook/config.yml` dosyasını oluşturur:

```yaml
config-version: 1
collection:
  enabled: true
```

Eski dosyada yalnızca `collect-spawner-drops: true|false` varsa değeri ilk açılışta `collection.enabled` alanına taşınır. İki anahtar birlikte varsa yeni `collection.enabled` alanı önceliklidir; geçişten sonra eski anahtar silinir. Geçiş kullanıcı dosyasındaki değerleri okur, paket varsayılanlarıyla üzerine yazmaz. Toplamayı kapatmak için `collection.enabled: false` yap ve sunucuyu yeniden başlat.

Üç bağımsız hook aynı yaşam döngüsü düzenini kullanır: altın/sarı robot bannerı gerçek plugin adı, Maven sürümü ve platformu gösterir; ardından yapılandırma, bağımlılık ve bağlantı adımları raporlanır. Yalnızca etkin bileşenler hazırsa süreli başarı mesajı yazılır. `collection.enabled: false` durumu başarı yerine `Pasif` olarak bildirilir; başlangıç hatası exception ile raporlanır. Kapanış önce yeni aktarımı engeller, ardından yalnızca hook'un kendi dinleyicisini ve görevini durdurup kuyruğu boşaltır. Tamamlanmamış başlangıç veya temizlik hatası başarılı kapanış olarak gösterilmez. Bu küçük log yardımcısı her JAR'ın kendi kaynaklarına dahildir; ortak plugin veya Maven modülü kurulmaz. PlugMan ile söküp takma desteklenmez; tam sunucu yeniden başlatması kullan.

## Ayar rehberi

### Hangi ayar hangi dosyada?

| Dosya / bölüm | Sorumluluk |
| --- | --- |
| `plugins/AxSpawnerFarmerHook/config.yml` | Yalnızca köprünün toplama anahtarı ve config şeması. [Paket varsayılanı](src/main/resources/config.yml). |
| `plugins/uxmFarmer/config.yml` | Farmer lisansı, bölge/ekonomi/NPC bağlantısı ve dünya izinleri. Hook bu dosyayı yönetmez. |
| `plugins/uxmFarmer/collected-materials.yml` | Toplanabilecek ürünler, fiyatlar, level ve kapasite tanımları. |
| `plugins/uxmFarmer/general-settings.yml` | Farmer'ın genel toplama kuralları; örneğin `collect-custom-items`. |
| AxSpawners'ın kendi ayarları | Spawner loot üretimi ve Ax deposu kapasitesi. Hook yeni loot üretmez. |

### Köprünün bütün ayarları

| Anahtar | Tür / varsayılan | Etkisi |
| --- | --- | --- |
| `config-version` | Tam sayı / `1` | Hook'un config şemasıdır, plugin sürümü değildir. Elle artırma; bu sürüm yalnızca `1` kabul eder. Eksikse geçiş sırasında eklenir. |
| `collection.enabled` | YAML boolean / `true` | `true`: yeni spawner loot'unu Farmer kurallarıyla işler. `false`: listener/task oluşturmaz ve `Pasif` bildirir; mevcut depoyu taşımaz veya silmez. |

`true` ve `false` tırnaksız olmalı. `"false"` metni, sayı veya boş bir YAML bölümü doğru boolean yerine geçmez; hatalı türle hook başlamaz. `yes/no` gibi YAML sürümüne göre farklı yorumlanan yazımlar yerine açıkça `true/false` kullan. `collection` bir YAML bölümü olmalıdır. Hook'un kendi komutu, reload komutu, oyuncu izni, veritabanı veya Redis ayarı yoktur. Bu iki alanı değiştirdikten sonra tam yeniden başlatma gerekir.

### Önce uxmFarmer'ı çalışır hale getir

1. uxmFarmer'ın ürettiği `config.yml` içindeki `license-key` alanını kendi geçerli lisansınla doldur. Anahtarı GitHub issue'ya veya config örneğine yazma. `License key is not set! Plugin will not start.` görülüyorsa önce bu engeli çöz; hook bunu aşamaz.
2. `hooks.region-hook` alanına gerçekten kurulu bölge/ada entegrasyonunu seç. Varsayılan `uxmClaims` olduğu için başka ada eklentisi kullanırken olduğu gibi bırakma. Spawner ile Farmer aynı çözümlenebilir bölgeye bağlı olmalı; yalnızca birbirlerine yakın olmaları yeterli değildir.
3. `hooks.money-hook` ve `hooks.interaction-hook` değerlerini kurulu eklentilerle eşleştir. Örneğin `Vault` seçimi çalışan bir ekonomi sağlayıcısı da gerektirir. Varsayılan `FancyNpcs` bir hook gereksinimi değildir; NPC etkileşimi istemiyorsan Farmer'ın desteklediği `interaction-hook: default` seçeneği komut erişimi içindir.
4. Dünya filtresini kontrol et. Farmer 3.18.1 paket varsayılanında `list-type: BLACKLIST` ve `world-list: [world]` vardır; bu, `world` dünyasını **engeller**. Test dünyasını açıkça izinli yapmak için aşağıdaki **Farmer configine ait** bölümü mevcut dosyayla birleştirebilirsin:

```yaml
world-management:
  list-type: WHITELIST
  world-list:
    - world
```

Bu parça hook configine yazılmaz. Dünya adını kendi sunucuna göre değiştir; diğer Farmer ayarlarını silme.

### Ürün, Farmer ve kapasite ayarları

Farmer menüsünden Farmer'ı ve doğal eşya toplama seçeneğini aç. İlgili ürünün Farmer içindeki toplama düğmesi de açık olmalıdır. Hook, ürünün `Material` adına göre canlı Farmer tanımını kullanır; ayrıca bir ürün izin listesi tutmaz.

Örneğin `BLAZE_ROD` toplamak için mevcut `plugins/uxmFarmer/collected-materials.yml` dosyasındaki **tek** `collected-materials` bölümüne şu ürünü ekleyebilirsin. Örnek değerler ekonomi önerisi değildir; kendi level/fiyat politikanla uyumlu düzenle:

```yaml
collected-materials:
  BLAZE_ROD:
    sellable: true
    unit-price: 1.0
    levels:
      0:
        price: 0
        capacity: 2500
```

Mevcut `collected-materials` bölümünü ikinci kez yazma veya diğer ürünlerle değiştirme. Farmer'ın desteklenen yeniden yükleme yolunu ya da tam yeniden başlatmayı kullan. Hook sonraki aktarımda güncel ürün tanımlarını okur; ürün tanımını değiştirmek mevcut Farmer stoğunu otomatik taşımak anlamına gelmez.

Özel isim/lore/enchant taşıyan eşyalar `general-settings.yml` içindeki `collect-custom-items: false` kuralıyla reddedilebilir. Üründe `model-data` varsa eşleşme gerekir; üründe model-data tanımı yokken model-data'lı eşya da kabul edilmez. Bu kuralları yalnızca bir ürünü toplatmak için tüm sunucuda kontrolsüz açma.

Farmer kapasitesi doluysa kalan miktar AxSpawners deposuna yönlenir. Ax deposu da sınırlıdır; hook iki dolu depo için sınırsız saklama veya yere düşürme garantisi vermez.

<a id="ilk-test"></a>

## İlk test

1. Sunucuyu yeniden başlat. Önce AxSpawners ve uxmFarmer'ın etkin olduğunu, ardından hook'ta `Başarıyla başlatıldı` görüldüğünü doğrula. Yalnızca banner görmek hazır olduğu anlamına gelmez.
2. İzinli bir dünyada çalışan bölge entegrasyonu üzerinden bir Farmer kur. Aynı bölgeye bir spawner koy; ürün ve doğal eşya toplama açık, Farmer/Ax kapasitesi boş olsun.
3. Yeni üretimden önce Farmer ve Ax deposundaki ilgili ürün miktarını not al. Eski Ax stoklarının taşınmasını bekleme.
4. Bir yeni loot üretimini bekle. Farmer artışını ve kalan Ax artışını karşılaştır; başka satış/üretim/bonus hookları sonucu değiştiriyorsa onları da hesaba kat. Oyuncu envanterine eşya verilmesi beklenmez.
5. Farmer deposunu kontrollü şekilde doldurup yeni loot'un kalan kısmının Ax deposuna gittiğini kontrol et; Ax deposunu tamamen doldurmadan taşma testi yap.
6. `collection.enabled: false` ile yeniden başlatıp `Pasif` durumunu ve yeni loot'un köprü tarafından Farmer'a aktarılmadığını doğrula. Geri açmak için `true` yapıp yeniden başlat.

Bu hook yalnızca kurulduğu Paper backend'inde çalışır; Velocity'ye kurulmaz. Birden fazla backend'de kullanırken her birinde bağımlılıkları ve bölgeleri ayrı doğrula. Farmer'ın kendi çoklu sunucu/Redis ayarları bu hook tarafından kurulmaz veya değiştirilmez.


## Toplama kuralları

- Yalnızca yeni üretilen eşyalar işlenir. Mevcut spawner deposu ve XP aktarılmaz.
- Çiftçi, spawner'ın konumuna göre uxmFarmer'ın yapılandırılmış bölge entegrasyonu üzerinden seçilir.
- Dünya uxmFarmer tarafından izin verilen dünyalardan olmalıdır. Çiftçi ve çiftçinin doğal eşya toplama anahtarı açık olmalıdır.
- Yalnızca tanımlanmış ve etkin ürünler toplanır. Özel eşya ve model-data kısıtları korunur.
- Ürün listesi `uxmFarmer/collected-materials.yml` içindeki `collected-materials` bölümüdür. `SPIDER_EYE`, `PRISMARINE_SHARD`, `PRISMARINE_CRYSTALS` ve `BLAZE_ROD` için de aynı kural geçerlidir; ayrı bir izin listesi yoktur. Tanımsız ürünler çiftçiye eklenmez, AxSpawners deposuna bırakılır.
- uxmFarmer'ın mevcut depolama servisi kullanılır; toplama olayları, istatistikler, üretim ve otomatik satış davranışı korunur.
- Stack miktarı loot olayı sırasında sabitlenir; sırada beklerken spawner sayısı değişirse üretilmiş miktar değişmez. Çiftçiye sığmayan miktar bölünmeden AxSpawners deposuna yazılır, ardından slotlar ve GUI bir kez güncellenir.
- AxSpawners'ın asenkron ürettiği eşyalar bir sonraki sunucu tick'inde ana iş parçacığında işlenir; AxSpawners'ın spawner döngüsü bu işlemi beklemez. Çiftçiye sığmayan kısım aynı anda AxSpawners deposuna eklenir.
- Hook kapanırken sırada bekleyen eşyalar teslim edilir; kapanıştan sonra üretilen eşyalar doğrudan AxSpawners deposuna gider.
- Ayrı bir ürün configi tutulmaz. uxmFarmer kendi yapılandırmasını yeniden yüklediğinde hook sonraki aktarımda güncel ürün tanımlarını kullanır.
- Depo kapasitesi AxSpawners 1.12.1'in batch kurallarıyla uygulanır: başlangıçta limitin altındaysa kalan miktar eklenir; limitteyse yalnızca aynı ürünün son eksik stack'i tamamlanır; limit aşılmışsa kalan eklenmez. Hook yere eşya atmaz; dolu depo sınırsız depolama garantisi vermez.
## Sorun giderme

| Belirti | Kontrol |
| --- | --- |
| Çiftçi hiç eşya toplamıyor | Hook'un `collection.enabled` ayarını, çiftçinin doğal eşya toplama anahtarını ve spawner'ın bulunduğu bölgedeki çiftçiyi kontrol et. |
| Yalnızca bazı ürünler toplanmıyor | uxmFarmer ürün tanımını, ürünün açık olmasını ve varsa model-data eşleşmesini kontrol et. |
| Çiftçiye sığmayan eşyalar kayboluyor | AxSpawners deposunun kapasitesini kontrol et. Depo doluyken AxSpawners'ın kendi taşma davranışı geçerlidir. |
| Hook başlatılamıyor | Konsoldaki ilk hatayı incele; `collection.enabled` YAML boolean olmalı (`true` veya `false` kullan). AxSpawners ve uxmFarmer'ın etkin olduğundan emin ol. |
| `License key is not set` / Farmer kendini kapatıyor | `uxmFarmer/config.yml` içindeki geçerli lisansı tamamla; ardından tam yeniden başlat. Oto eritmenin beklemesi ve Ax hook'unun başlamaması bu bağımlılık engelinin sonucudur. |
| `world` dünyasında hiç çalışmıyor | Farmer'ın varsayılan `BLACKLIST` listesinde `world` bulunup bulunmadığını ve doğru bölge hook'unu kontrol et. |
| Plugin listesinde var, ama aktarım yok | Başarılı hazır mesajını kontrol et; configten kapalı hook `Pasif` olabilir. JAR'ın bulunması veya banner tek başına çalışma kanıtı değildir. |
| Otomatik üretilen eşyalar yere düşüyor | İncelenen AxSpawners 1.12.1 üretim/depo ekleme akışı yere eşya atmaz. Çalışan sunucunun AxSpawners jarı, diğer loot hookları ve spawner ayarları incelenmelidir; yalnızca uxmFarmer sürümünü güncellemek bu sorunun düzeldiğini kanıtlamaz. |

Sorun bildirirken sunucu, Java, AxSpawners ve uxmFarmer sürümleriyle ilgili konsol hatasını ekle. Token, parola ve oyuncu verilerini paylaşma.

1.0.1 review: asenkron aktarımda değişen spawner sayısının üretilmiş miktarı değiştirmesi ve kısmi çiftçi aktarımında erken slot güncellemesinin kalan eşyaları kaybettirmesi düzeltildi. Boş, bir slot limitli depoda 100 üretilen − 13 çiftçiye aktarılan = 87 depo senaryosu eski commit koduyla 64, yeni kodla 87 sonuç verdi.

1.1.0 doğrulaması: 24 test geçti. Ayrı smoke programında kapalı toplamanın listener/task oluşturmadığı, eski `false` değerinin korunduğu ve geçersiz config sonrasında güvenli kapanışta başarı mesajı yazılmadığı çalıştırıldı. Gerçek `StorageService.addProduct` kullanan izole aktarımda dört ürünün her biri için 40 eşya → 13 çiftçi + 27 Ax deposu, canlı ürün tanımı kaldırılınca 40 Ax deposu sonucu gözlendi. Asenkron kuyruk kapanışında 18 birim bir kez teslim edildi; sonraki 13 birim üreticide kaldı. Dünya, bölge ve kalıcı ürün nesneleri test doubles kullandı; canlı sunucudaki dünya/bölge aktarımı ve yere düşme sorunu bu çalıştırmada doğrulanmadı.

## Lisans

Hook kaynakları **GPL-3.0-only** ile lisanslıdır. Ayrıntılar: [LICENSE](LICENSE).

AxSpawners, uxmFarmer ve bunlara ait varlıklar kendi lisanslarına tabidir; bu kaynak dağıtımının parçası değildir.

