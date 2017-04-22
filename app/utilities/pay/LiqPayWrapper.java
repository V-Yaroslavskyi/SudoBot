package utilities.pay;

import com.liqpay.LiqPay;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class LiqPayWrapper {

    private final static String PUBLIC_KEY="i74554005793";
    private final static String PRIVATE_KEY="grQNyGRHnHpFRJrDehuH7nKsw6RfCl0IsZK06xbN";

    public static String pay(String chatId, String phone, String uniqueOrderId) throws Exception {
        Map<String, String> params = new HashMap<>();
        LiqPay liqpay = new LiqPay(PUBLIC_KEY, PRIVATE_KEY);

        params.put("action", "invoice_bot");
        params.put("version", "3");
        params.put("amount", "640");
        params.put("currency", "UAH");
        params.put("order_id", uniqueOrderId);
        params.put("channel_type", "telegram");
        params.put("account", chatId);
        params.put("phone", phone);

        return liqpay.api("request", params)
                .get("href").toString();
    }

    public static void main(String[] args) throws Exception {
        System.out.println(pay("123456", "+380939224914", UUID.randomUUID().toString()));
    }
}
