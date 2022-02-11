package me.maplef.mapbotv4.utils;

import org.apache.commons.lang.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class UrlUtils {

    public static final String QUESTION_MARK = "?";
    public static final String AND_MARK = "&";
    public static final String EQUAL_MARK = "=";

    public static LinkedHashMap<String, String> getParamsMap(String url){

        if(StringUtils.isBlank(url)){
            return null;
        }

        url = url.trim();
        int length = url.length();
        int index = url.indexOf(QUESTION_MARK);

        if(index > -1){
            if((length - 1) == index){
                return null;

            }else{
                String baseUrl = url.substring(0, index);
                String paramsString = url.substring(index + 1);

                if(!StringUtils.isBlank(paramsString)){
                    LinkedHashMap<String, String> paramsMap = new LinkedHashMap<>();
                    String[] params = paramsString.split(AND_MARK);

                    for (String param : params) {
                        if(!StringUtils.isBlank(param)){
                            String[] oneParam = param.split(EQUAL_MARK);
                            String paramName = oneParam[0];

                            if(!StringUtils.isBlank(paramName)){
                                if(oneParam.length > 1){
                                    paramsMap.put(paramName.trim(), oneParam[1]);

                                }else{
                                    paramsMap.put(paramName.trim(), "");
                                }
                            }

                        }
                    }
                    return paramsMap;
                }
            }
        }

        return null;
    }

    public static String addParams(String url, LinkedHashMap<String, String> params, boolean isOverride){
        if(StringUtils.isBlank(url)){
            return "";
        } else if (params == null || params.size() < 1){
            return url.trim();
        } else {
            url = url.trim();
            int index = url.indexOf(QUESTION_MARK);
            String baseUrl;

            if(index > -1){
                baseUrl = url.substring(0, index);

            }else{
                baseUrl = url;
            }

            LinkedHashMap<String, String> paramsMapInUrl = getParamsMap(url);

            if(paramsMapInUrl == null){
                paramsMapInUrl = new LinkedHashMap<>();
            }

            if(!isOverride){
                LinkedHashMap<String, String> newParams = new LinkedHashMap<>(params.size());

                for (Map.Entry<String, String> entry : params.entrySet()) {
                    if(!StringUtils.isBlank(entry.getKey())){
                        newParams.put(entry.getKey().trim(), entry.getValue());
                    }
                }

                for (Map.Entry<String, String> entry : newParams.entrySet()) {
                    for (Map.Entry<String, String> urlEntry : paramsMapInUrl.entrySet()) {
                        if(!StringUtils.isBlank(entry.getKey())){
                            if(entry.getKey().trim().equals(urlEntry.getKey())){
                                params.remove(entry.getKey().trim());
                            }
                        }
                    }
                }
            }


            for (Map.Entry<String, String> entry : params.entrySet()) {
                paramsMapInUrl.put(entry.getKey().trim(), entry.getValue());
            }

            if(paramsMapInUrl.size() > 0){
                StringBuilder paramBuffer = new StringBuilder(baseUrl);
                paramBuffer.append(QUESTION_MARK);
                Set<String> set = paramsMapInUrl.keySet();
                for (String paramName : set) {
                    paramBuffer.append(paramName).append(EQUAL_MARK)
                            .append(paramsMapInUrl.get(paramName) == null ? "" : paramsMapInUrl.get(paramName))
                            .append(AND_MARK);
                }
                paramBuffer.deleteCharAt(paramBuffer.length() - 1);
                return paramBuffer.toString();
            }
            return baseUrl;

        }
    }

    public static String addParam(String url, String name, String value, boolean isOverride){
        if(StringUtils.isBlank(url)){
            return "";

        }else if(StringUtils.isBlank(name)){
            return url.trim();

        }else{
            LinkedHashMap<String, String> params = new LinkedHashMap<>();
            params.put(name.trim(), value);
            return addParams(url, params, isOverride);
        }
    }


    /**
     * 向url链接追加参数(单个) （会覆盖已经有的参数）
     * @param url 链接地址
     * @param name String 参数名
     * @param value String 参数值
     */
    public static String addParam(String url, String name, String value){
        return addParam(url, name, value, true);
    }


    /**
     * 向url链接追加参数(单个) （不会覆盖已经有的参数）
     * @param url 链接地址
     * @param name String 参数名
     * @param value String 参数值
     */
    public static String addParamNotExist(String url, String name, String value){
        return addParam(url, name, value, false);
    }


    /**
     * 向url链接追加参数（会覆盖已经有的参数）
     * @param url 链接地址
     * @param params LinkedHashMap<String, String> 参数
     */
    public static String addParams(String url, LinkedHashMap<String, String> params){
        return addParams(url, params, true);
    }


    /**
     * 向url链接追加参数（不会覆盖已经有的参数）
     * @param url 链接地址
     * @param params LinkedHashMap<String, String> 参数
     */
    public static String addParamsNotExist(String url, LinkedHashMap<String, String> params){
        return addParams(url, params, false);
    }


    /**
     * 移除url链接的多个参数 
     * @param url String 链接地址
     * @param paramNames String... 参数
     */
    public static String removeParams(String url, String... paramNames){
        if(StringUtils.isBlank(url)){
            return "";

        }else if(paramNames == null || paramNames.length < 1){
            return url.trim();

        }else{
            url = url.trim();
            int length = url.length();
            int index = url.indexOf(QUESTION_MARK);

            if(index > -1){
                if((length - 1) == index){
                    return url;
                }else{
                    LinkedHashMap<String, String> paramsMap = getParamsMap(url);

                    if(paramsMap != null && paramsMap.size() > 0){
                        for (String paramName : paramNames) {
                            if(!StringUtils.isBlank(paramName)){
                                paramsMap.remove(paramName.trim());
                            }
                        }
                    }

                    String baseUrl = url.substring(0, index);

                    //重新拼接链接
                    if(paramsMap != null && paramsMap.size() > 0){
                        StringBuilder paramBuffer = new StringBuilder(baseUrl);
                        paramBuffer.append(QUESTION_MARK);
                        Set<String> set = paramsMap.keySet();
                        for (String paramName : set) {
                            paramBuffer.append(paramName).append(EQUAL_MARK).append(paramsMap.get(paramName)).append(AND_MARK);
                        }
                        paramBuffer.deleteCharAt(paramBuffer.length() - 1);
                        return paramBuffer.toString();
                    }
                    return baseUrl;
                }
            }
            return url;
        }
    }
}