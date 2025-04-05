# gRPC

## 資源定位

* gRPC的資源定位完整應該是ip + port + method_name，gRPC的默認端口號是50051。所以server側要暴露50051端口，client也要和50051端口建立連結

## why http/2

* http/2支持Multiplexing，同一個tcp連結可以雙向，多請求共用。如果用raw tcp，每一次rpc請求就要建立一個新的tcp連結，建立連結很耗時
* http/2支持Bidirectional Streaming，http/2已經能復用同一個tcp連結實現雙向邏輯流傳輸。如果使用raw tcp，雖然tcp支持full-duplex，但要用戶側自己實現邏輯流區分
* http/2的Flow Control做得更好。不僅能control individual stream，還能control overall connection level。
* http/2支持HPACK，而raw tcp不支持

## why not http/3 

* 2016年gRPC誕生時，http/3在實驗中不穩定
* tcp的基建搭得最好，firewalls, load balancers, proxies, NAT devices, operating system network stacks等對tcp優化很好。QUiC基於udp，基建搭得不行，比如older NAT devices or load balancers
* 支持的語言當時還少
* 性能差距沒想像的那麼大。QUIC在**lossy or high-latency networks** (like mobile networks or the public internet)的網絡下性能會更好。但gRPC一般用於微服務(**low-latency, reliable datacenter networks**)，這種情況下兩者性能差距不明顯。
* 現在已經有gRPC已經支持http/3了可以用，並不是一定要http/2

## 性能優勢

1. protobuf序列化傳輸性能比json高
2. 基於http/2