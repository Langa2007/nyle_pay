import React from 'react';
import Hero from '../components/Hero';
import Ecosystem from '../components/Ecosystem';
import MerchantRegistration from '../components/MerchantRegistration';
import SandboxTester from '../components/SandboxTester';
import Security from '../components/Security';
import Footer from '../components/Footer';

const Home = () => {
  return (
    <div>
      <Hero />
      <Ecosystem />
      <MerchantRegistration />
      <SandboxTester />
      <Security />
      <Footer />
    </div>
  );
};

export default Home;
