package com.by.sasa.bistrovic.ethereumandnfc.ethereum.utils;

import com.by.sasa.bistrovic.ethereumandnfc.ethereum.bean.EthBalanceBean;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


/**
 * Utils class used to interact with Ethereum Blockchain.
 */
public class EthereumUtils {

    /**
     * this method reads the balance by ether address and returns a balance object.
     *
     * @param ethAddress
     * @return EthBalanceBean containing information about the balance
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public static EthBalanceBean getBalance(String ethAddress, String url) throws Exception {
        Web3j web3 = Web3j.build(new HttpService(url));

        BigInteger wei = getBalanceFromApi(web3, ethAddress, DefaultBlockParameterName.LATEST);
        if (wei == null) {
            wei = new BigInteger("0");
        }
        BigDecimal ether = Convert.fromWei(wei.toString(), Convert.Unit.ETHER);

        BigInteger unconfirmedWei = getBalanceFromApi(web3, ethAddress, DefaultBlockParameterName.PENDING);
        if (unconfirmedWei == null) {
            unconfirmedWei = new BigInteger("0");
        }
        unconfirmedWei = unconfirmedWei.subtract(wei);
        BigDecimal unconfirmedEther = Convert.fromWei(unconfirmedWei.toString(), Convert.Unit.ETHER);

        return new EthBalanceBean(wei, ether, unconfirmedWei, unconfirmedEther);
    }

    /**
     * this method reads the ether balance from api via web3    j
     *
     * @param web3                      used web3j
     * @param ethAddress                ether address
     * @param defaultBlockParameterName
     * @return the ether balance itself
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private static BigInteger getBalanceFromApi(Web3j web3, String ethAddress,
                                                DefaultBlockParameterName defaultBlockParameterName) throws Exception {
        BigInteger wei = null;
        EthGetBalance ethGetBalance = web3
                .ethGetBalance(ethAddress, defaultBlockParameterName)
                .sendAsync().get(10, TimeUnit.SECONDS);
        //EthGetBalance ethGetBalance = web3.ethGetBalance(ethAddress, DefaultBlockParameterName.LATEST).send();
        if (ethGetBalance != null) {
            wei = ethGetBalance.getBalance();
        }

        return wei;
    }

}
