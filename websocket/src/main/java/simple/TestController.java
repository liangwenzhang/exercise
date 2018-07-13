package simple;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Created by wenliang_zhang on 18-7-13.
 */
@Controller
public class TestController {
    @RequestMapping("/hello")
    public String test(Model model){
        return "index";
    }
}
