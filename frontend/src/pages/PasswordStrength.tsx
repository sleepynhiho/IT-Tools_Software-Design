import { useState } from "react";
import {
  Box,
  IconButton,
  Paper,
  TextField,
  Typography,
  InputAdornment,
} from "@mui/material";

import FavoriteIcon from "@mui/icons-material/Favorite";

import Visibility from "@mui/icons-material/Visibility";
import VisibilityOff from "@mui/icons-material/VisibilityOff";

const PasswordStrength = () => {
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);

  // Hàm tính toán độ mạnh mật khẩu (giả lập)
  const calculateStrength = (pwd: string) => {
    if (!pwd)
      return {
        timeToCrack: "Instantly",
        length: 0,
        entropy: 0,
        charSet: 0,
        score: 0,
      };

    const length = pwd.length;
    const charSet = new Set(pwd).size; // Đếm số lượng ký tự khác nhau
    const entropy = Math.log2(charSet ** length);
    const score = Math.min(100, Math.round((entropy / 10) * 100));

    // Giả lập thời gian bẻ khóa
    let timeToCrack = "Instantly";
    if (entropy > 30) timeToCrack = "Minutes";
    if (entropy > 40) timeToCrack = "Hours";
    if (entropy > 50) timeToCrack = "Days";
    if (entropy > 60) timeToCrack = "Years";

    return { timeToCrack, length, entropy: entropy.toFixed(2), charSet, score };
  };

  const strength = calculateStrength(password);

  const [liked, setLiked] = useState(false);

  return (
    <Box
      component="main"
      sx={{
        display: "flex",
        alignContent: "center",
        justifyContent: "center",
        height: "100%",
        width: "100%",
      }}
    >
      <Box
        sx={{
          p: 1,
          maxWidth: 600,
          height: "400px",
          display: "flex",
          flexDirection: "column",
          justifyContent: "flex-start",
        }}
      >
        <Box
          sx={{
            display: "flex",
            justifyContent: "space-between",
            alignContent: "center",
          }}
        >
          <Typography variant="h4" sx={{ fontWeight: "bold" }}>
            Password strength analyser
          </Typography>

          <IconButton
            onClick={(e) => {
              e.stopPropagation();
              setLiked(!liked);
            }}
          >
            {liked ? (
              <FavoriteIcon color="error" />
            ) : (
              <FavoriteIcon sx={{ color: "custom.icon" }} />
            )}
          </IconButton>
        </Box>

        <Box sx={{ display: "flex", justifyContent: "flex-start", my: 1 }}>
          <Box
            sx={{ width: "200px", height: "1px", backgroundColor: "#a1a1a1" }}
          />
        </Box>

        <Typography sx={{ color: "gray", mb: 3 }}>
          Discover the strength of your password with this client-side-only
          password strength analyser and crack time estimation tool.
        </Typography>

        <TextField
          fullWidth
          type={showPassword ? "text" : "password"}
          placeholder="Enter a password..."
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          sx={{
            bgcolor: "#333333",
            color: "white",
            borderRadius: 1,
            "& .MuiInputBase-root": { height: "40px" },
            "& input": { color: "white", padding: "10px" },
            "& .MuiOutlinedInput-root": {
              "& fieldset": { borderColor: "#666" }, // Màu viền mặc định
              "&:hover fieldset": { borderColor: "lightgreen" }, // Khi hover
              "&.Mui-focused fieldset": { borderColor: "lightgreen" }, // Khi focus
            },
          }}
          InputProps={{
            endAdornment: (
              <InputAdornment position="end">
                <IconButton
                  onClick={() => setShowPassword(!showPassword)}
                  sx={{ color: "white" }}
                >
                  {showPassword ? <VisibilityOff /> : <Visibility />}
                </IconButton>
              </InputAdornment>
            ),
          }}
        />

        {/* Kết quả phân tích */}
        <Paper
          elevation={3}
          sx={{
            p: 3,
            bgcolor: "#212121",
            color: "white",
            borderRadius: 2,
            mt: 2,
          }}
        >
          <Typography sx={{ textAlign: "center", mb: 1 }}>
            Duration to crack this password with brute force
          </Typography>
          <Typography
            variant="h5"
            sx={{ textAlign: "center", fontWeight: "bold" }}
          >
            {strength.timeToCrack}
          </Typography>
        </Paper>

        <Paper
          elevation={3}
          sx={{
            p: 3,
            bgcolor: "#212121",
            color: "white",
            borderRadius: 2,
            mt: 2,
          }}
        >
          <Typography>Password length: {strength.length}</Typography>
          <Typography>Entropy: {strength.entropy}</Typography>
          <Typography>Character set size: {strength.charSet}</Typography>
          <Typography>Score: {strength.score} / 100</Typography>
        </Paper>

        <Typography sx={{ mt: 2, fontSize: "14px", color: "gray" }}>
          <b>Note:</b> The computed strength is based on the time it would take
          to crack the password using a brute force approach, it does not take
          into account the possibility of a dictionary attack.
        </Typography>
      </Box>
    </Box>
  );
};

export default PasswordStrength;

// import { useState } from "react";
// import {
//   Box,
//   Typography,
//   Switch,
//   Slider,
//   TextField,
//   Button,
//   Paper,
//   IconButton,
// } from "@mui/material";
// import ContentCopyIcon from "@mui/icons-material/ContentCopy";
// import RefreshIcon from "@mui/icons-material/Refresh";
// import FavoriteIcon from "@mui/icons-material/Favorite";

//   const [liked, setLiked] = useState(false);

//   return (
//     <Box
//       component="main"
//       sx={{
//         display: "flex",
//         alignContent: "center",
//         justifyContent: "center",
//         height: "100%",
//         width: "100%",
//       }}
//     >
//       <Box
//         sx={{
//           p: 1,
//           maxWidth: 600,
//           height: "400px",
//           display: "flex",
//           flexDirection: "column",
//           justifyContent: "flex-start",
//         }}
//       >
//         <Box
//           sx={{
//             display: "flex",
//             justifyContent: "space-between",
//             alignContent: "center",
//           }}
//         >
//           <Typography variant="h4" sx={{ fontWeight: "bold" }}>
//             Token Generator
//           </Typography>

//           <IconButton
//             onClick={(e) => {
//               e.stopPropagation();
//               setLiked(!liked);
//             }}
//           >
//             {liked ? (
//               <FavoriteIcon color="error" />
//             ) : (
//               <FavoriteIcon sx={{ color: "custom.icon" }} />
//             )}
//           </IconButton>
//         </Box>

//         <Box sx={{ display: "flex", justifyContent: "flex-start", my: 1 }}>
//           <Box
//             sx={{ width: "200px", height: "1px", backgroundColor: "#a1a1a1" }}
//           />
//         </Box>

//         <Typography sx={{ color: "gray", mb: 3 }}>
//           Generate random string with the chars you want, uppercase or lowercase
//           letters, numbers and/or symbols.
//         </Typography>

//         <Paper
//           elevation={3}
//           sx={{ p: 3, bgcolor: "#212121", color: "white", borderRadius: 2 }}
//         >
//         </Paper>
//       </Box>
//     </Box>
//   );
// };
