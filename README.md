# AxSpawnerFarmerHook

**Türkçe** · [**English**](README.en.md)

![Java 21+](https://img.shields.io/badge/Java-21%2B-F4BD60?style=flat-square)
![Paper API 1.21.4](https://img.shields.io/badge/Paper_API-1.21.4-55C7F4?style=flat-square)
[![GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-4ADB9B?style=flat-square)](LICENSE)

AxSpawners'ın yeni ürettiği eşyaları, spawner'ın bulunduğu bölgedeki uxmFarmer'a aktaran bağımsız Paper eklentisi. AxSpawners veya uxmFarmer üzerinde değişiklik gerektirmez.

![Yeni spawner eşyalarının çiftçiye aktarımı ve kalan miktarın AxSpawners deposuna yönlendirilmesi](assets/collection-flow.gif)

*Temsili işleyiş animasyonu; oyun içi kayıt değildir.*

[Gereksinimler](#gereksinimler) · [Kurulum](#kurulum) · [Derleme](#derleme) · [Toplama kuralları](#toplama-kuralları) · [Sorun giderme](#sorun-giderme)

## Gereksinimler

- Java 21 veya üzeri.
- Paper; proje 1.21.4 API'sine karşı derlenir. Paper 26.2 henüz doğrulanmadı. Folia desteklenmez.
- AxSpawners 1.12.1 ve uxmFarmer 3.18.1-SNAPSHOT.
- Kaynaktan derlemek için Maven 3.

Java 21 bytecode'u Java 25 ile uyumludur; bu, belirli bir sunucu sürümünün desteklendiği anlamına gelmez.

## Derleme

AxSpawners ve uxmFarmer'ı kendi lisans koşullarına uygun şekilde ayrıca edin. Bu eklentilerin jarları ve kaynakları bu repoda dağıtılmaz.

Jarları `lib/AxSpawners-1.12.1.jar` ve `uxmFarmer-3.18.1-SNAPSHOT.jar` konumlarına koyup yerel Maven deposuna yükle:

```sh
mvn install:install-file -Dfile=lib/AxSpawners-1.12.1.jar -DgroupId=com.artillexstudios -DartifactId=AxSpawners -Dversion=1.12.1 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=uxmFarmer-3.18.1-SNAPSHOT.jar -DgroupId=com.uxplima -DartifactId=uxmFarmer -Dversion=3.18.1-SNAPSHOT -Dpackaging=jar -DgeneratePom=true
mvn clean verify
```

Çıktı: `target/AxSpawnerFarmerHook.jar`. Bağımlılık jarları bu dosyaya eklenmez.

## Kurulum

1. AxSpawners ve uxmFarmer'ı, kendi gerekli bağımlılıklarıyla birlikte kurup etkinleştir.
2. `AxSpawnerFarmerHook.jar` dosyasını sunucunun `plugins` klasörüne koy.
3. Sunucuyu yeniden başlat.

Hook, `plugins/AxSpawnerFarmerHook/config.yml` dosyasını oluşturur:

```yaml
collect-spawner-drops: true
```

Toplamayı kapatmak için `false` yap. Ayarı değiştirdikten sonra sunucuyu yeniden başlat.

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
| Çiftçi hiç eşya toplamıyor | Hook'un `collect-spawner-drops` ayarını, çiftçinin doğal eşya toplama anahtarını ve spawner'ın bulunduğu bölgedeki çiftçiyi kontrol et. |
| Yalnızca bazı ürünler toplanmıyor | uxmFarmer ürün tanımını, ürünün açık olmasını ve varsa model-data eşleşmesini kontrol et. |
| Çiftçiye sığmayan eşyalar kayboluyor | AxSpawners deposunun kapasitesini kontrol et. Depo doluyken AxSpawners'ın kendi taşma davranışı geçerlidir. |
| Hook başlatılamıyor | Konsoldaki ilk hatayı incele. AxSpawners ve uxmFarmer'ın etkin olduğundan ve sunucunun gereksinimleri karşıladığından emin ol. |
| Otomatik üretilen eşyalar yere düşüyor | İncelenen AxSpawners 1.12.1 üretim/depo ekleme akışı yere eşya atmaz. Çalışan sunucunun AxSpawners jarı, diğer loot hookları ve spawner ayarları incelenmelidir; yalnızca uxmFarmer sürümünü güncellemek bu sorunun düzeldiğini kanıtlamaz. |

Sorun bildirirken sunucu, Java, AxSpawners ve uxmFarmer sürümleriyle ilgili konsol hatasını ekle. Token, parola ve oyuncu verilerini paylaşma.

1.0.1 review: asenkron aktarımda değişen spawner sayısının üretilmiş miktarı değiştirmesi ve kısmi çiftçi aktarımında erken slot güncellemesinin kalan eşyaları kaybettirmesi düzeltildi. Boş, bir slot limitli depoda 100 üretilen − 13 çiftçiye aktarılan = 87 depo senaryosu eski commit koduyla 64, yeni kodla 87 sonuç verdi.

Doğrulama: 20 test geçti. Gerçek `StorageService.addProduct` ile izole smoke çalıştırmasında dört ürün için kısmi çiftçi/depo aktarımı ve ürün tanımı kaldırılınca tamamının depoya gitmesi doğrulandı. Kuyruk kapanışında üretim anındaki stack miktarıyla teslim de çalıştırıldı. Paper dünya/bölge nesneleri test doubles kullandı; canlı Leaf 26.2 sunucusunda yere düşme sorunu henüz doğrulanmadı.

## Lisans

Hook kaynakları **GPL-3.0-only** ile lisanslıdır. Ayrıntılar: [LICENSE](LICENSE).

AxSpawners, uxmFarmer ve bunlara ait varlıklar kendi lisanslarına tabidir; bu kaynak dağıtımının parçası değildir.

