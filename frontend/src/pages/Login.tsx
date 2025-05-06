import React, { useState, ChangeEvent, FormEvent, JSX } from "react";
import { useNavigate, Link as RouterLink } from "react-router-dom";
import {
  Container,
  Box,
  TextField,
  Button,
  Typography,
  Avatar,
  CssBaseline,
  Link as MuiLink,
  Grid,
  CircularProgress,
  SxProps,
  Theme,
  Fade,
  Paper,
  IconButton,
  Divider,
  InputAdornment,
  useTheme,
  alpha,
} from "@mui/material";
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import HomeIcon from "@mui/icons-material/Home";
import VisibilityIcon from "@mui/icons-material/Visibility";
import VisibilityOffIcon from "@mui/icons-material/VisibilityOff";
import EmailIcon from "@mui/icons-material/Email";
import { AuthError, signInWithEmailAndPassword } from "firebase/auth";
import { auth } from "../firebaseConfig";

// Current date and time information
const CURRENT_DATE_TIME = "2025-05-06 17:42:16";
const CURRENT_USER_LOGIN = "hanhiho";

// Define the types for form data and errors
interface FormData {
  email: string;
  password: string;
}

interface FormErrors {
  email?: string;
  password?: string;
  general?: string;
}

// Re-use the gradient from SignUpPage
const pageBackgroundGradient: string =
  "linear-gradient(48deg,#1f525a 0%,#337d5e 60%,#108048 100%)";

// Define styles using SxProps for type safety
const pageBackgroundStyles: SxProps<Theme> = {
  minHeight: "100vh",
  background: pageBackgroundGradient,
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  p: 3,
  position: "relative",
};

const formBoxStyles = (theme: Theme): SxProps<Theme> => ({
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  bgcolor: alpha("#222", 0.9),
  padding: { xs: 3, sm: 4 },
  borderRadius: 3,
  boxShadow: "0 8px 32px rgba(0,0,0,0.2)",
  backdropFilter: "blur(8px)",
  width: "100%",
  maxWidth: "450px",
  position: "relative",
  overflow: "hidden",
  "&::before": {
    content: '""',
    position: "absolute",
    top: 0,
    left: 0,
    right: 0,
    height: "4px",
    background: "linear-gradient(90deg, #3b956f, #1ea54c, #3b956f)",
  },
});

const textFieldStyles = (theme: Theme): SxProps<Theme> => ({
  "& .MuiOutlinedInput-root": {
    borderRadius: 2,
    backgroundColor: alpha(theme.palette.background.paper, 0.6),
    transition: theme.transitions.create([
      "border-color",
      "background-color",
      "box-shadow",
    ]),
    "&:hover": {
      backgroundColor: alpha(theme.palette.background.paper, 0.8),
    },
    "&.Mui-focused": {
      backgroundColor: alpha(theme.palette.background.paper, 0.8),
      boxShadow: `0 0 0 2px ${alpha(theme.palette.primary.main, 0.25)}`,
    },
  },
  "& .MuiInputLabel-root.Mui-focused": {
    color: theme.palette.primary.main,
  },
  "& .MuiInputAdornment-root": {
    color: theme.palette.text.secondary,
  },
});

const homeButtonStyles: SxProps<Theme> = {
  position: "absolute",
  top: 16,
  left: 16,
  backgroundColor: "rgba(255, 255, 255, 0.2)",
  backdropFilter: "blur(8px)",
  color: "white",
  "&:hover": {
    backgroundColor: "rgba(255, 255, 255, 0.3)",
  },
};

function LoginPage(): JSX.Element {
  const navigate = useNavigate();
  const theme = useTheme();
  const [formData, setFormData] = useState<FormData>({
    email: "",
    password: "",
  });
  const [errors, setErrors] = useState<FormErrors>({});
  const [loading, setLoading] = useState<boolean>(false);
  const [showPassword, setShowPassword] = useState<boolean>(false);

  const validate = (): boolean => {
    const tempErrors: FormErrors = {};
    tempErrors.email = !formData.email
      ? "Email is required."
      : !/\S+@\S+\.\S+/.test(formData.email)
      ? "Email is not valid."
      : undefined;
    tempErrors.password = !formData.password
      ? "Password is required."
      : undefined;

    const filteredErrors = Object.entries(tempErrors).reduce(
      (acc, [key, value]) => {
        if (value) {
          acc[key as keyof FormErrors] = value;
        }
        return acc;
      },
      {} as FormErrors
    );

    setErrors(filteredErrors);
    return Object.keys(filteredErrors).length === 0;
  };

  const handleChange = (
    event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    const { name, value } = event.target;

    if (name in formData) {
      setFormData((prevData) => ({
        ...prevData,
        [name]: value,
      }));

      // Clear specific field error on change
      const errorKey = name as keyof FormErrors;
      if (errors[errorKey]) {
        setErrors((prevErrors) => ({
          ...prevErrors,
          [errorKey]: undefined,
        }));
      }
      // Also clear general error when user types again
      if (errors.general) {
        setErrors((prevErrors) => ({ ...prevErrors, general: undefined }));
      }
    }
  };

  const toggleShowPassword = () => {
    setShowPassword((prev) => !prev);
  };

  const getFirebaseErrorMessage = (code: string): string => {
    switch (code) {
      case "auth/user-not-found":
        return "No account found with this email address.";
      case "auth/wrong-password":
        return "Incorrect password. Please try again.";
      case "auth/invalid-email":
        return "Please enter a valid email address.";
      case "auth/user-disabled":
        return "This account has been disabled. Please contact support.";
      case "auth/too-many-requests":
        return "Too many failed login attempts. Please try again later.";
      default:
        console.error(
          `[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: Unhandled Firebase Auth Error Code:`,
          code
        );
        return "Login failed. Please check your credentials and try again.";
    }
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setErrors({});

    if (!validate()) {
      console.log(
        `[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: Local Validation Failed`
      );
      return;
    }

    setLoading(true);

    try {
      console.log(
        `[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: Attempting Firebase login for: ${formData.email}`
      );
      const userCredential = await signInWithEmailAndPassword(
        auth,
        formData.email,
        formData.password
      );

      console.log(
        `[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: Firebase User Logged In Successfully:`,
        userCredential.user.uid
      );

      navigate("/");
    } catch (error) {
      console.error(
        `[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: Firebase Login Error:`,
        error
      );
      if (error instanceof Error && "code" in error) {
        const firebaseError = error as AuthError;
        setErrors({ general: getFirebaseErrorMessage(firebaseError.code) });
      } else {
        setErrors({ general: "An unknown error occurred during login." });
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box sx={pageBackgroundStyles}>
      <CssBaseline />

      {/* Home button floating in top-left corner */}
      <IconButton
        aria-label="Back to home"
        sx={homeButtonStyles}
        onClick={() => navigate("/")}
      >
        <HomeIcon />
      </IconButton>

      <Fade in={true} timeout={800}>
        <Container component="main" maxWidth="xs">
          <Paper sx={formBoxStyles(theme)} elevation={0}>
            <Avatar
              sx={{
                m: 1,
                width: 56,
                height: 56,
                bgcolor: "primary.main",
                boxShadow: "0 4px 12px rgba(0,0,0,0.15)",
              }}
            >
              <LockOutlinedIcon fontSize="large" />
            </Avatar>

            <Typography
              component="h1"
              variant="h4"
              sx={{
                fontWeight: "bold",
                letterSpacing: 0.5,
                my: 1.5,
              }}
            >
              Log In
            </Typography>

            {/* Display general errors */}
            {errors.general && (
              <Fade in={!!errors.general}>
                <Typography
                  color="error"
                  variant="body2"
                  sx={{
                    mt: 1,
                    mb: 2,
                    textAlign: "center",
                    backgroundColor: alpha(theme.palette.error.main, 0.1),
                    borderRadius: 1,
                    padding: "10px 16px",
                    width: "100%",
                    fontWeight: "medium",
                  }}
                >
                  {errors.general}
                </Typography>
              </Fade>
            )}

            <Box
              component="form"
              onSubmit={handleSubmit}
              noValidate
              sx={{ mt: 1.5, width: "100%" }}
            >
              {/* Email Field */}
              <TextField
                margin="normal"
                required
                fullWidth
                id="email"
                label="Email Address"
                name="email"
                autoComplete="email"
                autoFocus
                value={formData.email}
                onChange={handleChange}
                error={!!errors.email}
                helperText={errors.email}
                disabled={loading}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <EmailIcon fontSize="small" />
                    </InputAdornment>
                  ),
                }}
                sx={textFieldStyles(theme)}
              />

              {/* Password Field */}
              <TextField
                margin="normal"
                required
                fullWidth
                name="password"
                label="Password"
                type={showPassword ? "text" : "password"}
                id="password"
                autoComplete="current-password"
                value={formData.password}
                onChange={handleChange}
                error={!!errors.password}
                helperText={errors.password}
                disabled={loading}
                InputProps={{
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton
                        aria-label="toggle password visibility"
                        onClick={toggleShowPassword}
                        edge="end"
                        size="small"
                      >
                        {showPassword ? (
                          <VisibilityOffIcon fontSize="small" />
                        ) : (
                          <VisibilityIcon fontSize="small" />
                        )}
                      </IconButton>
                    </InputAdornment>
                  ),
                }}
                sx={textFieldStyles(theme)}
              />

              <Button
                type="submit"
                fullWidth
                variant="contained"
                disabled={loading}
                sx={{
                  mt: 3,
                  mb: 2,
                  py: 1.5,
                  position: "relative",
                  fontWeight: "bold",
                  fontSize: "1rem",
                  textTransform: "none",
                  borderRadius: 2,
                  background: "linear-gradient(90deg, #3b956f, #1ea54c)",
                  boxShadow: "0 4px 12px rgba(59, 149, 111, 0.3)",
                  "&:hover": {
                    background: "linear-gradient(90deg, #358b66, #1c9845)",
                    boxShadow: "0 6px 14px rgba(59, 149, 111, 0.4)",
                  },
                }}
              >
                {/* Loading Spinner */}
                {loading && (
                  <CircularProgress
                    size={24}
                    sx={{
                      color: "primary.contrastText",
                      position: "absolute",
                      top: "50%",
                      left: "50%",
                      marginTop: "-12px",
                      marginLeft: "-12px",
                    }}
                  />
                )}
                {loading ? "Loging In..." : "Log In"}
              </Button>

              <Divider sx={{ my: 2 }}>
                <Typography variant="body2" color="text.secondary">
                  OR
                </Typography>
              </Divider>

              <Grid
                container
                spacing={1}
                justifyContent="center"
                sx={{ mt: 1 }}
              >
                <Grid item>
                  <Typography variant="body2" sx={{ textAlign: "center" }}>
                    Don't have an account?{" "}
                    <MuiLink
                      component={RouterLink}
                      to="/signup"
                      sx={{
                        fontWeight: "medium",
                        color: "primary.main",
                        textDecoration: "none",
                        "&:hover": {
                          textDecoration: "underline",
                        },
                      }}
                    >
                      Sign Up
                    </MuiLink>
                  </Typography>
                </Grid>
              </Grid>
            </Box>
          </Paper>
        </Container>
      </Fade>
    </Box>
  );
}

export default LoginPage;
