package ${packageName};

import com.asiainfo.bits.core.i18n.I18NCode;
import com.asiainfo.ams.acctcomp.exception.IAcctException;

/**
* @author Convertor
*/

public enum ${className} implements IAcctException {
<#list list as value>
    /** ${value.message} */
    @I18NCode("${value.message}")
    ${value.name},

</#list>
    ;

}
