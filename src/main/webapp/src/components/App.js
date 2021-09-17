import React, { Component } from 'react';
import { Link } from 'react-router-dom';
import logo from '../assets/Israeli_blue_Star_of_David.png';

class App extends Component {
    state = { walletInfo: {} };

    componentDidMount() {
        fetch(`${document.location.origin}/W4LL37/1Nf0`, {method: 'POST'})
        // fetch(`http://localhost:8018/W4LL37/1Nf0`, {method: 'POST'}) 
            .then(response => response.json())
            .then(json => this.setState({ walletInfo: json }));
    }

    render() {
        const { address, balance } = this.state.walletInfo;

        return (
            <div className='App'>
                <img className='logo' src={logo}></img>
                <br/>
                <div>
                    Welcome <a href="https://www.youtube.com/watch?v=s4CRvAPzxjI" target="_self">Through the Blood of The Lamb!</a>
                </div>
                <br/>
                <div><Link to={'/blocks'}>Blocks</Link></div>
                <div><Link to={'/conduct-transaction'}>Conduct a Transaction</Link></div>
                <div><Link to={'/transaction-pool'}>Transaction Pool</Link></div>
                <br/>
                <div className='WalletInfo'>
                    <div>Address: {address}</div>
                    <div>Balance: {balance}</div>
                </div>
            </div>
        );
    }
}

export default App;