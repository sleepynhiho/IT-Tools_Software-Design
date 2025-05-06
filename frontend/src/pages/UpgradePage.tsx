import React from 'react';
import { Container, Box, Typography, Button, Paper, Grid, Divider } from '@mui/material';
import StarIcon from '@mui/icons-material/Star';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const UpgradePage = () => {
  const navigate = useNavigate();
  const { userType } = useAuth();
  const isPremium = userType === 'premium' || userType === 'admin';

  // Redirect if already premium
  if (isPremium) {
    setTimeout(() => {
      navigate('/');
    }, 5000);
  }
  
  const handleUpgradeClick = () => {
    // Implement your payment flow here
    alert('This would connect to your payment gateway');
  };

  return (
    <Container maxWidth="lg" sx={{ py: 8 }}>
      {isPremium ? (
        <Paper elevation={3} sx={{ p: 4, textAlign: 'center', background: 'linear-gradient(48deg,#25636c 0%,#3b956f 60%,#14a058 100%)' }}>
          <StarIcon sx={{ fontSize: 60, color: '#ffb300', mb: 2 }} />
          <Typography variant="h4" gutterBottom sx={{ color: 'white' }}>
            You Already Have Premium Access!
          </Typography>
          <Typography variant="body1" sx={{ mb: 4, color: 'white' }}>
            You will be redirected to the home page in a few seconds...
          </Typography>
          <Button 
            variant="contained" 
            onClick={() => navigate('/')}
            sx={{ 
              backgroundColor: '#ffffff', 
              color: '#3b956f',
              '&:hover': { 
                backgroundColor: '#f0f0f0' 
              } 
            }}
          >
            Return Home Now
          </Button>
        </Paper>
      ) : (
        <>
          <Box sx={{ textAlign: 'center', mb: 6 }}>
            <Typography variant="h3" component="h1" gutterBottom sx={{ 
              fontWeight: 'bold', 
              background: 'linear-gradient(48deg,#25636c 0%,#3b956f 60%,#14a058 100%)',
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'transparent'
            }}>
              Upgrade to Premium
            </Typography>
            <Typography variant="h6" sx={{ color: '#a3a3a3', maxWidth: '800px', mx: 'auto' }}>
              Unlock all premium tools and features to maximize your experience
            </Typography>
          </Box>
          
          <Grid container spacing={4} justifyContent="center">
            <Grid item xs={12} md={6}>
              <Paper elevation={3} sx={{ 
                p: 4, 
                height: '100%',
                border: '1px solid #282828',
                backgroundColor: "#1c1c1c",
              }}>
                <Typography variant="h5" gutterBottom sx={{ display: 'flex', alignItems: 'center' }}>
                  <StarIcon sx={{ mr: 1, color: '#ffb300' }} />
                  Premium Plan
                </Typography>
                <Typography variant="h3" sx={{ mb: 2 }}>
                  $9.99<Typography component="span" variant="body1" sx={{ color: '#a3a3a3' }}>/month</Typography>
                </Typography>
                <Typography variant="body2" sx={{ color: '#a3a3a3', mb: 3 }}>
                  Billed monthly, cancel anytime
                </Typography>
                
                <Divider sx={{ my: 3, borderColor: 'rgba(255, 255, 255, 0.12)' }} />
                
                <Box sx={{ mb: 3 }}>
                  <Box sx={{ display: 'flex', mb: 2 }}>
                    <CheckCircleOutlineIcon sx={{ mr: 2, color: '#1ea54c' }} />
                    <Typography>Access to all premium tools</Typography>
                  </Box>
                  <Box sx={{ display: 'flex', mb: 2 }}>
                    <CheckCircleOutlineIcon sx={{ mr: 2, color: '#1ea54c' }} />
                    <Typography>Priority customer support</Typography>
                  </Box>
                  <Box sx={{ display: 'flex', mb: 2 }}>
                    <CheckCircleOutlineIcon sx={{ mr: 2, color: '#1ea54c' }} />
                    <Typography>Early access to new features</Typography>
                  </Box>
                  <Box sx={{ display: 'flex', mb: 2 }}>
                    <CheckCircleOutlineIcon sx={{ mr: 2, color: '#1ea54c' }} />
                    <Typography>No usage limitations</Typography>
                  </Box>
                  <Box sx={{ display: 'flex' }}>
                    <CheckCircleOutlineIcon sx={{ mr: 2, color: '#1ea54c' }} />
                    <Typography>Regular feature updates</Typography>
                  </Box>
                </Box>
                
                <Button 
                  variant="contained" 
                  fullWidth 
                  size="large"
                  onClick={handleUpgradeClick}
                  sx={{ 
                    background: "linear-gradient(48deg,#25636c 0%,#3b956f 60%,#14a058 100%)",
                    "&:hover": { 
                      background: "linear-gradient(48deg,#1f525a 0%,#337d5e 60%,#108048 100%)" 
                    },
                    py: 1.5
                  }}
                >
                  Upgrade Now
                </Button>
              </Paper>
            </Grid>
            
            <Grid item xs={12} md={6}>
              <Paper elevation={3} sx={{ 
                p: 4, 
                height: '100%',
                border: '1px solid #282828',
                backgroundColor: "#1c1c1c", 
              }}>
                <Typography variant="h5" gutterBottom>Why Go Premium?</Typography>
                
                <Box sx={{ my: 3 }}>
                  <Typography variant="h6" gutterBottom>Access Exclusive Tools</Typography>
                  <Typography variant="body2" sx={{ color: '#a3a3a3', mb: 3 }}>
                    Our premium tools provide advanced capabilities that help you work faster and smarter.
                  </Typography>
                  
                  <Typography variant="h6" gutterBottom>Premium Support</Typography>
                  <Typography variant="body2" sx={{ color: '#a3a3a3', mb: 3 }}>
                    Get priority response from our support team when you need assistance.
                  </Typography>
                  
                  <Typography variant="h6" gutterBottom>No Limitations</Typography>
                  <Typography variant="body2" sx={{ color: '#a3a3a3' }}>
                    Premium users enjoy unlimited access with no usage caps or throttling.
                  </Typography>
                </Box>
                
                <Box sx={{ mt: 4, p: 2, backgroundColor: 'rgba(30, 165, 76, 0.1)', borderRadius: '8px' }}>
                  <Typography variant="body2" sx={{ fontStyle: 'italic', color: '#a3a3a3', textAlign: 'center' }}>
                    "Upgrading to premium unlocked so many powerful features that have dramatically improved my workflow!"
                  </Typography>
                  <Typography variant="body2" sx={{ mt: 1, color: '#1ea54c', textAlign: 'center', fontWeight: 'bold' }}>
                    - Happy Premium User
                  </Typography>
                </Box>
                
                <Button 
                  variant="text"
                  fullWidth
                  onClick={() => navigate('/')}
                  sx={{ mt: 3, color: '#a3a3a3' }}
                >
                  Return to Home
                </Button>
              </Paper>
            </Grid>
          </Grid>
        </>
      )}
    </Container>
  );
};

export default UpgradePage;