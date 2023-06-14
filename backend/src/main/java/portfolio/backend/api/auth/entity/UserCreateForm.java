package portfolio.backend.api.auth.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
public class UserCreateForm {
    
    //회원가입할 때 나타나는 양식
    @Size(min = 3, max = 25)
    @NotBlank(message = "사용자 아이디(이메일)는 필수 입력 값입니다.")
    private String username; //login할 때 id

    @Size(min = 3, max = 25)
    @NotEmpty(message = "창작자 고유 아이디는 필수 입력 값 입니다.")
    private String user_id; //창작자 고유 id

    @NotEmpty(message = "비밀번호는 필수 입력 값입니다.")
    @Length(min = 4, max = 16, message = "비밀번호는 4자 이상, 16자 이하로 입력해주세요")
    private String password1;

    @NotEmpty(message = "비밀번호 확인은 필수항목입니다.")
    private String password2;

    @NotEmpty(message = "사용자 이름(first_name)은 필수 입력 값입니다.")
    private String first_name;

    @NotEmpty(message = "사용자 성(last_name)은 필수 입력 값입니다.")
    private String last_name;

    @NotEmpty(message = "아티스트 유형은 필수항목입니다.")
    private String artist_type;


    
}
