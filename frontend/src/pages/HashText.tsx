import { useState } from "react";
import CryptoJS from "crypto-js";
import {
  Box,
  Typography,
  TextField,
  Paper,
  MenuItem,
  Select,
  FormControl,
  InputLabel,
  Button,
  IconButton,
} from "@mui/material";
import ContentCopyIcon from "@mui/icons-material/ContentCopy";
import FavoriteIcon from "@mui/icons-material/Favorite";

const HashText = () => {
  const [text, setText] = useState("");
  const [encoding, setEncoding] = useState("hex");
  const [hashedValues, setHashedValues] = useState<{ [key: string]: string }>(
    {}
  );

  const algorithms = [
    "MD5",
    "SHA1",
    "SHA256",
    "SHA224",
    "SHA512",
    "SHA384",
    "SHA3",
    "RIPEMD160",
  ];

  const [liked, setLiked] = useState(false);

  // Hàm băm dữ liệu với thuật toán cụ thể
  const hashText = (algo: string) => {
    if (!text) return "";
    let hash;

    switch (algo) {
      case "MD5":
        hash = CryptoJS.MD5(text);
        break;
      case "SHA1":
        hash = CryptoJS.SHA1(text);
        break;
      case "SHA256":
        hash = CryptoJS.SHA256(text);
        break;
      case "SHA224":
        hash = CryptoJS.SHA224(text);
        break;
      case "SHA512":
        hash = CryptoJS.SHA512(text);
        break;
      case "SHA384":
        hash = CryptoJS.SHA384(text);
        break;
      case "SHA3":
        hash = CryptoJS.SHA3(text);
        break;
      case "RIPEMD160":
        hash = CryptoJS.RIPEMD160(text);
        break;
      default:
        return "";
    }

    return encoding === "hex"
      ? hash.toString(CryptoJS.enc.Hex)
      : encoding === "base64"
      ? hash.toString(CryptoJS.enc.Base64)
      : encoding === "base64url"
      ? hash
          .toString(CryptoJS.enc.Base64)
          .replace(/\+/g, "-")
          .replace(/\//g, "_")
          .replace(/=+$/, "")
      : hash.toString();
  };

  // Cập nhật giá trị băm khi nhập văn bản
  const handleTextChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setText(e.target.value);
    const newHashes = algorithms.reduce((acc, algo) => {
      acc[algo] = hashText(algo);
      return acc;
    }, {} as { [key: string]: string });

    setHashedValues(newHashes);
  };

  // Sao chép giá trị băm vào clipboard
  const copyToClipboard = (value: string) => {
    navigator.clipboard.writeText(value);
  };

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
          height: "auto",
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
          <Typography variant="h5" sx={{ fontWeight: "bold" }}>
            Hash Text
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

        <Typography sx={{ color: "gray", mb: 2 }}>
          Hash a text string using MD5, SHA1, SHA256, SHA224, SHA512, SHA384,
          SHA3, or RIPEMD160.
        </Typography>

        <Paper
          elevation={3}
          sx={{ p: 2, bgcolor: "#212121", color: "white", borderRadius: 2 }}
        >
          <Typography sx={{ mb: 1 }}>Your text to hash:</Typography>
          <TextField
            fullWidth
            placeholder="Your string to hash..."
            multiline
            value={text}
            onChange={handleTextChange}
            sx={{ bgcolor: "#333", borderRadius: 1, mb: 2 }}
            InputProps={{ sx: { color: "white" } }}
          />

          <FormControl fullWidth sx={{ mb: 2 }}>
            <InputLabel sx={{ color: "white" }}>Digest encoding</InputLabel>
            <Select
              value={encoding}
              onChange={(e) => setEncoding(e.target.value)}
              sx={{ bgcolor: "#333", color: "white" }}
            >
              <MenuItem value="hex">Hexadecimal (base 16)</MenuItem>
              <MenuItem value="base64">Base64 (base 64)</MenuItem>
              <MenuItem value="base64url">
                Base64URL (base 64 URL-safe)
              </MenuItem>
            </Select>
          </FormControl>

          {/* Algorithm List */}
          {algorithms.map((algo) => (
            <Box
              key={algo}
              display="flex"
              alignItems="center"
              justifyContent="space-between"
              sx={{
                mb: 1,
                p: 0.5,
                borderRadius: 1,
                bgcolor: "#292929",
                transition: "0.3s",
                "&:hover": { bgcolor: "#333" },
              }}
            >
              <Button
                variant="contained"
                size="small"
                sx={{
                  bgcolor: "#36ad6a",
                  "&:hover": { bgcolor: "#2e8b57" },
                  minWidth: 90,
                  py: 0.5,
                  fontSize: 12,
                }}
              >
                {algo}
              </Button>
              <TextField
                fullWidth
                size="small"
                value={hashedValues[algo] || ""}
                sx={{
                  bgcolor: "#444",
                  borderRadius: 1,
                  mx: 1,
                  input: { color: "white", fontSize: 12 },
                }}
                InputProps={{ readOnly: true }}
              />
              <IconButton
                size="small"
                onClick={() => copyToClipboard(hashedValues[algo])}
              >
                <ContentCopyIcon sx={{ color: "white" }} fontSize="small" />
              </IconButton>
            </Box>
          ))}
        </Paper>
      </Box>
    </Box>
  );
};

export default HashText;
